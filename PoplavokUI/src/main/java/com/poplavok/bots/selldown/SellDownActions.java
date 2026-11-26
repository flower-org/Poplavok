package com.poplavok.bots.selldown;

import com.poplavok.bots.selldown.SellDownBotState.BotOrder;
import com.poplavok.bots.selldown.SellDownBotState.Event;
import com.poplavok.bots.selldown.SellDownBotState.EventType;
import com.poplavok.bots.selldown.SellDownBotState.Level;
import com.poplavok.bots.selldown.SellDownBotState.OrderEventType;
import com.poplavok.bots.selldown.SellDownBotState.Status;
import com.poplavok.data.bot.BotExchange;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What {@link SellDownFlow}'s Steps do to a {@link SellDownBotState}. Queries and state changes are pure; the
 * {@code BotExchange} calls are thin: one exchange call, its result (or error) recorded in the returned state.
 * <p>
 * Exchange calls never throw: an error is recorded as an ERROR event and reported by {@link Outcome#failed()}.
 */
final class SellDownActions {
    private SellDownActions() {
    }

    /** New state after an exchange action; {@code failed}: the exchange call failed (recorded as an ERROR event). */
    record Outcome(SellDownBotState state, boolean failed) {
    }

    // ---------- Queries ----------

    /** Finished, with no order left open: nothing more to do. */
    static boolean isDone(SellDownBotState state) {
        return state.isFinished() && openLevels(state).isEmpty();
    }

    /** Levels with an open order (SELLING or BUYING_BACK). */
    static List<Level> openLevels(SellDownBotState state) {
        return state.levels().stream().filter(l -> l.orderId() != null).toList();
    }

    /** IDs of the open orders, to watch for status changes. */
    static Set<String> openOrderIds(SellDownBotState state) {
        return openLevels(state).stream().map(l -> Objects.requireNonNull(l.orderId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static boolean isBuyingBack(SellDownBotState state) {
        return state.levels().stream().anyMatch(l -> l.status() == Status.BUYING_BACK);
    }

    /** SELLING levels whose order the bot hasn't asked to cancel yet. */
    static List<Level> sellsToCancel(SellDownBotState state) {
        return state.levels().stream()
                .filter(l -> l.status() == Status.SELLING && !isCancelRequested(state, l))
                .toList();
    }

    static boolean hasOpenSells(SellDownBotState state) {
        return state.levels().stream().anyMatch(l -> l.status() == Status.SELLING);
    }

    /** True if the price is at or above the break-even trigger (manual, else the planned one if available). */
    static boolean isBreakEvenDue(SellDownBotState state, @Nullable BigDecimal price) {
        SellDownBotState.BreakEven breakEven = state.breakEven();
        return price != null && breakEven != null && breakEven.trigger() != null
                && price.compareTo(breakEven.trigger()) >= 0;
    }

    /** PENDING levels whose sell trigger the price is at or below. */
    static List<Level> triggeredSells(SellDownBotState state, @Nullable BigDecimal price) {
        if (price == null) {
            return List.of();
        }
        return state.levels().stream()
                .filter(l -> l.status() == Status.PENDING
                        && price.compareTo(new BigDecimal(state.plan(l).sellTriggerPrice())) <= 0)
                .toList();
    }

    static boolean isCancelRequested(SellDownBotState state, Level level) {
        String orderId = level.orderId();
        BotOrder recorded = orderId == null ? null : state.order(orderId);
        return recorded != null && recorded.isCancelRequested();
    }

    // ---------- Pure state changes ----------

    /**
     * Applies the exchange's view of a level's order: records it if changed, and moves the level once it's closed.
     * Filled: SELLING to SOLD, BUYING_BACK to BROKEN_EVEN. Cancelled: SELLING back to PENDING, BUYING_BACK back to
     * SOLD; filled parts stay as they are.
     */
    static SellDownBotState track(SellDownBotState state, Level level, BotExchange.Order order, Instant now) {
        BotOrder recorded = state.order(order.orderId());
        if (recorded != null && !recorded.changedBy(order)) {
            return state;
        }
        OrderEventType type = order.isDone() ? OrderEventType.CLOSED : OrderEventType.UPDATED;
        state = state.withOrder(recorded == null
                ? BotOrder.placed(level.number(), order, now).with(order, type, now)
                : recorded.with(order, type, now));
        if (!order.isDone()) {
            return state;
        }
        boolean selling = level.status() == Status.SELLING;
        if (order.status() == BotExchange.OrderStatus.FILLED) {
            state = orderEvent(state, EventType.ORDER_FILLED, level.number(), order, now);
            state = state.withLevel(level.withStatus(selling ? Status.SOLD : Status.BROKEN_EVEN, null));
            return event(state, now, selling ? EventType.SOLD : EventType.BROKEN_EVEN, level.number(),
                    null, null, null, null, null);
        }
        state = orderEvent(state, EventType.ORDER_CANCELLED, level.number(), order, now);
        return state.withLevel(level.withStatus(selling ? Status.PENDING : Status.SOLD, null));
    }

    /**
     * Applies a fresh view of the orders: {@link #track}s each order still on a level, records an error for each
     * failed fetch of one. Orders no level has any more are ignored.
     */
    static SellDownBotState apply(SellDownBotState state, OrderStatuses.Snapshot snapshot, Instant now) {
        for (BotExchange.Order order : snapshot.orders()) {
            Level level = levelWithOrder(state, order.orderId());
            if (level != null) {
                state = track(state, level, order, now);
            }
        }
        for (Map.Entry<String, Exception> failure : snapshot.failures().entrySet()) {
            Level level = levelWithOrder(state, failure.getKey());
            if (level != null) {
                state = error(state, now, level.number(), "getOrder", "getOrder " + failure.getKey(),
                        failure.getValue());
            }
        }
        return state;
    }

    private static @Nullable Level levelWithOrder(SellDownBotState state, String orderId) {
        return state.levels().stream().filter(l -> orderId.equals(l.orderId())).findFirst().orElse(null);
    }

    /**
     * Sets (or, with null, clears) the bot-wide manual break-even trigger; its buy price is the trigger plus the fill
     * buffer.
     *
     * @throws IllegalStateException    if the bot is finished
     * @throws IllegalArgumentException if {@code trigger} isn't positive
     */
    static SellDownBotState setManualBreakEven(SellDownBotState state, @Nullable BigDecimal trigger, Instant now) {
        checkManualBreakEven(state, trigger);
        String price = trigger == null ? null : plain(state.breakEvenPriceForTrigger(trigger));
        state = state.withManualBreakEven(trigger == null ? null : plain(trigger), price);
        return event(state, now, EventType.BREAK_EVEN_SET, null, price, null, null, null,
                trigger == null ? "manual break-even cleared" : "manual break-even set: trigger " + plain(trigger));
    }

    /** Throws as {@link #setManualBreakEven} would, without changing anything. */
    static void checkManualBreakEven(SellDownBotState state, @Nullable BigDecimal trigger) {
        if (state.isFinished()) {
            throw new IllegalStateException("The bot is finished");
        }
        if (trigger != null && trigger.signum() <= 0) {
            throw new IllegalArgumentException("Break-even trigger must be positive");
        }
    }

    static SellDownBotState bind(SellDownBotState state, String exchangeName, Instant now) {
        return event(state.withExchangeName(exchangeName), now, EventType.BOUND, null, null, null, null, null,
                exchangeName);
    }

    static SellDownBotState finish(SellDownBotState state, Instant now) {
        return event(state, now, EventType.FINISHED, null, null, null, null, null, null);
    }

    // ---------- Exchange actions ----------

    // TODO: sell and buyBack can place a duplicate after a failure whose outcome is unknown. BotExchange makes up a
    //  clientOid per call and, if both of KucoinBotExchange's lookups by it fail, throws "may have been placed"; the
    //  bot doesn't keep that clientOid, so its next try sends a new one. Keep the clientOid on the level, and before
    //  placing again look it up (or reuse it).

    /**
     * Sells what the level still has left at its sell price: its BASE to sell minus what its earlier sells filled.
     * Nothing left, or a rest below the exchange minimum, counts as SOLD.
     */
    static Outcome sell(SellDownBotState state, Level level, BotExchange exchange, Instant now) {
        SellDownBotState.LevelPlan plan = state.plan(level);
        BigDecimal remaining = state.sold(level.number()).base();
        if (remaining.signum() <= 0) {
            state = state.withLevel(level.withStatus(Status.SOLD, null));
            return new Outcome(event(state, now, EventType.SOLD, level.number(), null, null, null, null,
                    "already sold by earlier orders"), false);
        }
        String note = remaining.compareTo(new BigDecimal(plan.baseToSell())) < 0 ? "sell of the rest" : "sell";
        BigDecimal price = new BigDecimal(plan.sellPrice());
        try {
            return new Outcome(placed(state, level, exchange.placeLimitOrder(state.definition().symbol(),
                    BotExchange.Side.SELL, price, remaining), Status.SELLING, price, remaining, note, now), false);
        } catch (BotExchange.OrderTooSmallException e) {
            state = state.withLevel(level.withStatus(Status.SOLD, null));
            return new Outcome(event(state, now, EventType.SOLD, level.number(), plain(price), plain(remaining),
                    null, null, "rest below the exchange minimum, counted as sold: " + e.getMessage()), false);
        } catch (IOException | RuntimeException e) {
            return failed(state, now, level.number(), "place sell", note, e);
        }
    }

    /**
     * Break-even buy of the latest SOLD level N: spends all the QUOTE levels N-1 and N received at the break-even
     * price (the manual one if set). Does nothing if there's no break-even price.
     */
    static Outcome buyBack(SellDownBotState state, BotExchange exchange, Instant now) {
        SellDownBotState.BreakEven breakEven = state.breakEven();
        if (breakEven == null || breakEven.price() == null) {
            return new Outcome(state, false);
        }
        Level level = state.levels().get(breakEven.level() - 1);
        BigDecimal price = breakEven.price();
        BigDecimal size = state.breakEvenSize(breakEven, price);
        String note = "break-even buy of levels " + (breakEven.level() - 1) + "-" + breakEven.level()
                + (breakEven.manualPrice() != null ? " at the manual price" : "");
        try {
            return new Outcome(placed(state, level, exchange.placeLimitOrder(state.definition().symbol(),
                    BotExchange.Side.BUY, price, size), Status.BUYING_BACK, price, size, note, now), false);
        } catch (IOException | RuntimeException e) {
            return failed(state, now, level.number(), "place buy", note, e);
        }
    }

    /** Asks the exchange to cancel the level's open order, unless already asked; the close is seen by {@link #apply}. */
    static Outcome cancel(SellDownBotState state, Level level, BotExchange exchange, Instant now) {
        String orderId = level.orderId();
        if (orderId == null || isCancelRequested(state, level)) {
            return new Outcome(state, false);
        }
        try {
            exchange.cancelOrder(state.definition().symbol(), orderId);
            BotOrder recorded = state.order(orderId);
            return new Outcome(recorded == null ? state : state.withOrder(recorded.cancelRequested(now)), false);
        } catch (IOException | RuntimeException e) {
            return failed(state, now, level.number(), "cancel", "cancel " + orderId, e);
        }
    }

    // ---------- Helpers ----------

    private static SellDownBotState placed(SellDownBotState state, Level level, BotExchange.Order order,
                                           Status status, BigDecimal price, BigDecimal size, String note,
                                           Instant now) {
        state = state.withLevel(level.withStatus(status, order.orderId()))
                .withOrder(BotOrder.placed(level.number(), order, now));
        return event(state, now, EventType.ORDER_PLACED, level.number(), plain(price), plain(size), null,
                order.orderId(), note);
    }

    /** Records an error; consecutive errors are counted in one ERROR event by "operation: exception class". */
    static SellDownBotState error(SellDownBotState state, Instant now, @Nullable Integer level, String operation,
                                  String note, Throwable e) {
        return state.withError(now.toString(), level, operation + ": " + e.getClass().getSimpleName(),
                note + ": " + e.getMessage());
    }

    private static Outcome failed(SellDownBotState state, Instant now, @Nullable Integer level, String operation,
                                  String note, Exception e) {
        return new Outcome(error(state, now, level, operation, note, e), true);
    }

    private static SellDownBotState orderEvent(SellDownBotState state, EventType type, int level,
                                               BotExchange.Order order, Instant now) {
        BigDecimal average = order.averageFillPrice();
        return event(state, now, type, level, plain(average == null ? order.price() : average),
                plain(order.filledSize()), plain(order.filledFunds()), order.orderId(),
                order.status() == BotExchange.OrderStatus.FILLED ? null
                        : "filled " + plain(order.filledSize()) + " of " + plain(order.size()));
    }

    private static SellDownBotState event(SellDownBotState state, Instant now, EventType type,
                                          @Nullable Integer level, @Nullable String price, @Nullable String base,
                                          @Nullable String quote, @Nullable String orderId, @Nullable String note) {
        return state.withEvent(new Event(now.toString(), type, level, price, base, quote, orderId, note));
    }

    static String plain(BigDecimal value) {
        return SellDownBotState.plain(value);
    }
}
