package com.poplavok.forms;

import com.poplavok.data.bot.BotExchange;
import com.poplavok.forms.SellDownBotState.Event;
import com.poplavok.forms.SellDownBotState.EventType;
import com.poplavok.forms.SellDownBotState.Level;
import com.poplavok.forms.SellDownBotState.LevelPlan;
import com.poplavok.forms.SellDownBotState.Status;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs one sell-down cycle of a {@link SellDownBotState} on a {@link BotExchange}:
 * <ol>
 * <li>Every PENDING level whose sell trigger the price is at/below gets a SELL limit
 * order for what it has left to sell at its sell price, so a sudden drop places several at once. SELLING, then SOLD
 * when filled.</li>
 * <li>Latest SOLD level N > 1: when the price is at/above its break-even trigger, all open sells are cancelled and a BUY
 * limit order is placed at its break-even price, spending all the QUOTE levels N-1 and N actually received. The price
 * is the planned one ({@link SellDownBotState#breakEven}; off if it would buy back less BASE than sold), or set by hand
 * via its trigger; with neither available, nothing happens.
 * BUYING_BACK, then BROKEN_EVEN when filled.</li>
 * <li>After break-even, or once all levels are SOLD, the cycle is finished.</li>
 * </ol>
 * Orders are tracked by polling ({@link #poll}). Every order is kept in the state with its own history (placed,
 * updated, cancel requested, closed); bot actions go to the state's history. Every change is passed to
 * {@code onChange} (e.g. to save the state file). Sizes and prices are as planned; the exchange rounds them in the
 * bot's favour. A rest to sell that's below the exchange minimum counts as sold.
 */
public class SellDownBot {
    private final BotExchange exchange;
    private final Clock clock;
    private final Consumer<SellDownBotState> onChange;
    private SellDownBotState state;
    private @Nullable BigDecimal lastPrice;
    private @Nullable BotExchange.Subscription priceSubscription;
    private @Nullable ScheduledFuture<?> pollTask;

    public SellDownBot(SellDownBotState state, BotExchange exchange, Clock clock, Consumer<SellDownBotState> onChange) {
        this.state = state;
        this.exchange = exchange;
        this.clock = clock;
        this.onChange = onChange;
    }

    public synchronized SellDownBotState state() {
        return state;
    }

    public synchronized boolean isRunning() {
        return pollTask != null;
    }

    /** Subscribes to the price and polls open orders every {@code pollMillis}. */
    public synchronized void start(ScheduledExecutorService scheduler, long pollMillis) throws IOException {
        if (pollTask != null) {
            return;
        }
        bind();
        priceSubscription = exchange.subscribePrice(state.definition().symbol(), this::onPrice);
        pollTask = scheduler.scheduleWithFixedDelay(this::poll, 0, pollMillis, TimeUnit.MILLISECONDS);
    }

    /** Binds an unbound bot to this exchange; refuses one bound to another. */
    private void bind() throws IOException {
        String bound = state.exchangeName();
        if (bound == null) {
            state = state.withExchangeName(exchange.exchangeName());
            event(EventType.BOUND, null, null, null, null, null, exchange.exchangeName());
            onChange.accept(state);
        } else if (!bound.equals(exchange.exchangeName())) {
            throw new IOException("bot is bound to " + bound + ", not " + exchange.exchangeName());
        }
    }

    /** Stops reacting; open orders stay on the exchange and are picked up again on the next start. */
    public synchronized void stop() {
        if (priceSubscription != null) {
            priceSubscription.close();
            priceSubscription = null;
        }
        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }
    }

    public synchronized void onPrice(BigDecimal price) {
        lastPrice = price;
        act();
    }

    /** Checks open orders, then acts on the last price. */
    public synchronized void poll() {
        for (Level level : state.levels()) {
            String orderId = level.orderId();
            if (orderId != null) {
                try {
                    track(level, exchange.getOrder(state.definition().symbol(), orderId));
                } catch (IOException | RuntimeException e) {
                    error(level.number(), "getOrder", "getOrder " + orderId, e);
                }
            }
        }
        act();
    }

    private void track(Level level, BotExchange.Order order) {
        SellDownBotState.BotOrder recorded = state.order(order.orderId());
        if (recorded != null && !recorded.changedBy(order)) {
            return;
        }
        SellDownBotState.OrderEventType type = order.isDone() ? SellDownBotState.OrderEventType.CLOSED
                : SellDownBotState.OrderEventType.UPDATED;
        state = state.withOrder(recorded == null ? SellDownBotState.BotOrder.placed(level.number(), order,
                clock.instant()).with(order, type, clock.instant()) : recorded.with(order, type, clock.instant()));
        if (!order.isDone()) {
            onChange.accept(state);
            return;
        }
        boolean selling = level.status() == Status.SELLING;
        if (order.status() == BotExchange.OrderStatus.FILLED) {
            event(EventType.ORDER_FILLED, level.number(), order);
            Status done = selling ? Status.SOLD : Status.BROKEN_EVEN;
            state = state.withLevel(level.withStatus(done, null));
            event(selling ? EventType.SOLD : EventType.BROKEN_EVEN, level.number(), null, null, null, null, null);
        } else {
            // Cancelled sell: back to PENDING; cancelled break-even buy: back to SOLD. Filled parts stay as they are.
            event(EventType.ORDER_CANCELLED, level.number(), order);
            state = state.withLevel(level.withStatus(selling ? Status.PENDING : Status.SOLD, null));
        }
        finishIfDone();
        onChange.accept(state);
    }

    /** Records FINISHED and stops once the cycle is finished and no order is open. */
    private void finishIfDone() {
        if (state.isFinished() && state.levels().stream().noneMatch(l -> l.orderId() != null)) {
            event(EventType.FINISHED, null, null, null, null, null, null);
            stop();
        }
    }

    private void act() {
        if (state.isFinished() || state.levels().stream().anyMatch(l -> l.status() == Status.BUYING_BACK)) {
            return;
        }
        BigDecimal price = lastPrice;
        SellDownBotState.BreakEven breakEven = state.breakEven();
        if (price != null && breakEven != null && breakEven.trigger() != null
                && price.compareTo(breakEven.trigger()) >= 0) {
            state.levels().stream().filter(l -> l.status() == Status.SELLING).forEach(this::cancel);
            breakEven(breakEven);
            return;
        }
        for (Level level : state.levels()) {
            LevelPlan plan = state.plan(level);
            if (level.status() == Status.PENDING && price != null
                    && price.compareTo(new BigDecimal(plan.sellTriggerPrice())) <= 0) {
                sell(level);
            }
        }
    }

    /** Sells what the level still has left: its BASE to sell minus what its earlier sells already filled. */
    private void sell(Level level) {
        LevelPlan plan = state.plan(level);
        BigDecimal remaining = state.sold(level.number()).base();
        if (remaining.signum() <= 0) {
            state = state.withLevel(level.withStatus(Status.SOLD, null));
            event(EventType.SOLD, level.number(), null, null, null, null, "already sold by earlier orders");
            finishIfDone();
            onChange.accept(state);
            return;
        }
        place(level, BotExchange.Side.SELL, new BigDecimal(plan.sellPrice()), remaining, Status.SELLING,
                remaining.compareTo(new BigDecimal(plan.baseToSell())) < 0 ? "sell of the rest" : "sell");
    }

    /** Spends all the QUOTE levels N-1 and N received on BASE, at the break-even price (the manual one if set). */
    private void breakEven(SellDownBotState.BreakEven breakEven) {
        Level level = state.levels().get(breakEven.level() - 1);
        BigDecimal price = Objects.requireNonNull(breakEven.price());
        place(level, BotExchange.Side.BUY, price, state.breakEvenSize(breakEven, price),
                Status.BUYING_BACK, "break-even buy of levels " + (breakEven.level() - 1) + "-" + breakEven.level()
                        + (breakEven.manualPrice() != null ? " at the manual price" : ""));
    }

    /**
     * Sets the bot-wide hand-set break-even trigger (buy price: trigger plus the fill buffer), overriding every
     * level's planned break-even; null clears it. If a break-even buy is open, it's cancelled and placed again (for
     * the QUOTE left) under the new setting.
     */
    public synchronized void setManualBreakEvenTrigger(@Nullable BigDecimal trigger) {
        if (state.isFinished()) {
            throw new IllegalStateException("The bot is finished");
        }
        if (trigger != null && trigger.signum() <= 0) {
            throw new IllegalArgumentException("Break-even trigger must be positive");
        }
        String price = trigger == null ? null : plain(state.breakEvenPriceForTrigger(trigger));
        state = state.withManualBreakEven(trigger == null ? null : plain(trigger), price);
        event(EventType.BREAK_EVEN_SET, null, price,
                null, null, null, trigger == null ? "manual break-even cleared"
                        : "manual break-even set: trigger " + plain(trigger));
        onChange.accept(state);
        Level buying = state.levels().stream().filter(l -> l.status() == Status.BUYING_BACK).findFirst().orElse(null);
        if (buying != null) {
            cancel(buying);
        } else if (pollTask != null) {
            act();
        }
    }

    private void place(Level level, BotExchange.Side side, BigDecimal price, BigDecimal size, Status status,
                       String note) {
        try {
            BotExchange.Order order = exchange.placeLimitOrder(state.definition().symbol(), side, price, size);
            state = state.withLevel(level.withStatus(status, order.orderId()))
                    .withOrder(SellDownBotState.BotOrder.placed(level.number(), order, clock.instant()));
            event(EventType.ORDER_PLACED, level.number(), plain(price), plain(size), null, order.orderId(), note);
            onChange.accept(state);
        } catch (BotExchange.OrderTooSmallException e) {
            if (side != BotExchange.Side.SELL) {
                error(level.number(), "place buy", note, e);
                return;
            }
            // Dust the exchange won't take: nothing more can be sold for this level
            state = state.withLevel(level.withStatus(Status.SOLD, null));
            event(EventType.SOLD, level.number(), plain(price), plain(size), null, null,
                    "rest below the exchange minimum, counted as sold: " + e.getMessage());
            finishIfDone();
            onChange.accept(state);
        } catch (IOException | RuntimeException e) {
            error(level.number(), "place " + side.name().toLowerCase(java.util.Locale.ROOT), note, e);
        }
    }

    private void cancel(Level level) {
        String orderId = level.orderId();
        SellDownBotState.BotOrder recorded = orderId == null ? null : state.order(orderId);
        if (orderId == null || (recorded != null && recorded.isCancelRequested())) {
            return;
        }
        try {
            exchange.cancelOrder(state.definition().symbol(), orderId);
            if (recorded != null) {
                state = state.withOrder(recorded.cancelRequested(clock.instant()));
                onChange.accept(state);
            }
        } catch (IOException | RuntimeException e) {
            error(level.number(), "cancel", "cancel " + orderId, e);
        }
    }

    /** Records an error; consecutive errors are counted in one ERROR event by "operation: exception class". */
    private void error(@Nullable Integer level, String operation, String note, Exception e) {
        state = state.withError(clock.instant().toString(), level, operation + ": " + e.getClass().getSimpleName(),
                note + ": " + e.getMessage());
        onChange.accept(state);
    }

    private void event(EventType type, int level, BotExchange.Order order) {
        BigDecimal average = order.averageFillPrice();
        event(type, level, plain(average == null ? order.price() : average), plain(order.filledSize()),
                plain(order.filledFunds()),
                order.orderId(), order.status() == BotExchange.OrderStatus.FILLED ? null
                        : "filled " + plain(order.filledSize()) + " of " + plain(order.size()));
    }

    private void event(EventType type, @Nullable Integer level, @Nullable String price, @Nullable String base,
                       @Nullable String quote, @Nullable String orderId, @Nullable String note) {
        state = state.withEvent(new Event(clock.instant().toString(), type, level, price, base, quote, orderId, note));
    }

    private static String plain(BigDecimal value) {
        return SellDownBotState.plain(value);
    }
}
