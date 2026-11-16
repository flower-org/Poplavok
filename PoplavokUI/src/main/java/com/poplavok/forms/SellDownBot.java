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
 * <li>Every PENDING level whose sell trigger the price is at/below (immediate levels: right away) gets a SELL limit
 * order for its BASE at its sell price, so a sudden drop places several at once. SELLING, then SOLD when filled.</li>
 * <li>Latest SOLD level N > 1: when the price is at/above its break-even trigger, all open sells are cancelled and a BUY
 * limit order is placed at its break-even price for the BASE sold by levels N-1 and N. BUYING_BACK, then BROKEN_EVEN
 * when filled.</li>
 * <li>After break-even, or once all levels are SOLD, the cycle is finished.</li>
 * </ol>
 * Orders are tracked by polling ({@link #poll}). Every order is kept in the state with its own history (placed,
 * updated, cancel requested, closed); bot actions go to the state's history. Every change is passed to
 * {@code onChange} (e.g. to save the state file). Sizes and prices are used as planned, without exchange rounding.
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
        priceSubscription = exchange.subscribePrice(state.definition().symbol(), this::onPrice);
        pollTask = scheduler.scheduleWithFixedDelay(this::poll, 0, pollMillis, TimeUnit.MILLISECONDS);
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
                    error(level.number(), "getOrder " + orderId + ": " + e.getMessage());
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
        if (state.isFinished() && state.levels().stream().noneMatch(l -> l.orderId() != null)) {
            event(EventType.FINISHED, null, null, null, null, null, null);
            stop();
        }
        onChange.accept(state);
    }

    private void act() {
        if (state.isFinished() || state.levels().stream().anyMatch(l -> l.status() == Status.BUYING_BACK)) {
            return;
        }
        BigDecimal price = lastPrice;
        Level latestSold = state.levels().stream().filter(l -> l.status() == Status.SOLD)
                .reduce((first, second) -> second).orElse(null);
        String breakEvenTrigger = latestSold == null ? null : state.plan(latestSold).breakEvenTriggerPrice();
        if (price != null && latestSold != null && breakEvenTrigger != null
                && price.compareTo(new BigDecimal(breakEvenTrigger)) >= 0) {
            state.levels().stream().filter(l -> l.status() == Status.SELLING).forEach(this::cancel);
            breakEven(latestSold);
            return;
        }
        for (Level level : state.levels()) {
            LevelPlan plan = state.plan(level);
            if (level.status() == Status.PENDING && (plan.immediate()
                    || (price != null && price.compareTo(new BigDecimal(plan.sellTriggerPrice())) <= 0))) {
                sell(level);
            }
        }
    }

    private void sell(Level level) {
        LevelPlan plan = state.plan(level);
        place(level, BotExchange.Side.SELL, new BigDecimal(plan.sellPrice()), new BigDecimal(plan.baseToSell()),
                Status.SELLING, "sell");
    }

    private void breakEven(Level level) {
        LevelPlan plan = state.plan(level);
        LevelPlan previous = state.plan(state.levels().get(level.number() - 2));
        BigDecimal size = new BigDecimal(previous.baseToSell()).add(new BigDecimal(plan.baseToSell()));
        place(level, BotExchange.Side.BUY, new BigDecimal(Objects.requireNonNull(plan.breakEvenPrice())),
                size, Status.BUYING_BACK, "break-even buy of levels " + previous.number() + "-" + level.number());
    }

    private void place(Level level, BotExchange.Side side, BigDecimal price, BigDecimal size, Status status,
                       String note) {
        try {
            BotExchange.Order order = exchange.placeLimitOrder(state.definition().symbol(), side, price, size);
            state = state.withLevel(level.withStatus(status, order.orderId()))
                    .withOrder(SellDownBotState.BotOrder.placed(level.number(), order, clock.instant()));
            event(EventType.ORDER_PLACED, level.number(), plain(price), plain(size), null, order.orderId(), note);
            onChange.accept(state);
        } catch (IOException | RuntimeException e) {
            error(level.number(), note + ": " + e.getMessage());
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
            error(level.number(), "cancel " + orderId + ": " + e.getMessage());
        }
    }

    private void error(@Nullable Integer level, String note) {
        event(EventType.ERROR, level, null, null, null, null, note);
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
