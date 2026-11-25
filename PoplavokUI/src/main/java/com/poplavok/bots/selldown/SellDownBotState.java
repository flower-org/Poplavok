package com.poplavok.bots.selldown;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.poplavok.data.bot.BotExchange;
import com.poplavok.data.utils.SellDownPlanner;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sell-down bot file (.bot), created by the approximator. {@code definition}: the plan frozen at creation, never
 * changed; the rest is the state of the bot acting upon it in a single sell-down cycle.
 * {@code levels}: each definition level's status, by level number.
 * {@code history}: the bot's actions, oldest first; missing in a file reads as empty.
 * {@code orders}: every order the bot placed, oldest first, each with its own history; missing reads as empty.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * {@code exchangeName}: the {@link BotExchange#exchangeName()} the bot is bound to on its first start; it won't run on any other.
 * {@code manualBreakEvenTrigger}: break-even trigger set by hand for the whole bot; overrides every level's planned
 * break-even until cleared. {@code manualBreakEvenPrice}: its buy price, worked out once when it's set (trigger plus
 * the fill buffer). Both null if none.
 */
public record SellDownBotState(Definition definition, List<Level> levels, List<Event> history, List<BotOrder> orders,
                               @Nullable String exchangeName, @Nullable String manualBreakEvenTrigger,
                               @Nullable String manualBreakEvenPrice) {
    public static final String EXTENSION = "*.bot";
    public static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public SellDownBotState {
        Objects.requireNonNull(definition, "definition");
        levels = levels == null ? List.of() : List.copyOf(levels);
        history = history == null ? List.of() : List.copyOf(history);
        orders = orders == null ? List.of() : List.copyOf(orders);
    }

    public SellDownBotState(Definition definition, List<Level> levels, List<Event> history, List<BotOrder> orders,
                            @Nullable String exchangeName) {
        this(definition, levels, history, orders, exchangeName, null, null);
    }

    /**
     * Immutable plan. Decimals are plain strings; ratios, buffer, slippage and fees are in %.
     * {@code price}, {@code debt}, {@code base}, {@code quote}: position the plan was calculated for.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Definition(String symbol, String createdAt, String price, String debt, String base, String quote,
                             String startRatio, String endRatio, String liquidationRatio,
                             String fillBuffer, String slippage, String makerFee, String takerFee,
                             List<LevelPlan> levels) {
        public Definition {
            levels = levels == null ? List.of() : List.copyOf(levels);
        }
    }

    /**
     * One planned sell level: sell {@code baseToSell} at {@code sellPrice}, starting at {@code sellTriggerPrice}.
     * Break-even prices are null for level 1.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LevelPlan(int number, String targetRatio, String sellPrice, String sellTriggerPrice,
                            String baseToSell, String expectedQuote,
                            @Nullable String breakEvenPrice, @Nullable String breakEvenTriggerPrice) {
    }

    /** The definition's plan for {@code level}. */
    public LevelPlan plan(Level level) {
        return definition.levels().stream().filter(plan -> plan.number() == level.number()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No level " + level.number() + " in the definition"));
    }

    /**
     * PENDING: not sold; SELLING: sell order open; SOLD: sell filled; BUYING_BACK: break-even buy open;
     * BROKEN_EVEN: break-even buy filled.
     */
    public enum Status { PENDING, SELLING, SOLD, BUYING_BACK, BROKEN_EVEN }

    public enum EventType {
        CREATED, BOUND, ORDER_PLACED, ORDER_FILLED, ORDER_CANCELLED, SOLD, BREAK_EVEN_SET, BROKEN_EVEN, FINISHED,
        ERROR
    }

    /**
     * One bot action. {@code time}: ISO-8601 instant. {@code level}: level number, null if not about a level.
     * {@code price}, {@code base}, {@code quote}: what was ordered/filled, where relevant. {@code orderId}: exchange
     * order, where relevant. {@code note}: free text, e.g. an error message.
     * <p>
     * ERROR events are statistical: consecutive errors are merged into one event. {@code errors}: how many of each
     * type; {@code time}/{@code lastTime}: first and latest; {@code note}: the latest message; {@code level}: null if
     * they were about different levels.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(String time, EventType type, @Nullable Integer level, @Nullable String price,
                        @Nullable String base, @Nullable String quote, @Nullable String orderId,
                        @Nullable String note, @Nullable String lastTime, @Nullable List<ErrorCount> errors) {
        public Event {
            errors = errors == null ? null : List.copyOf(errors);
        }

        public Event(String time, EventType type, @Nullable Integer level, @Nullable String price,
                     @Nullable String base, @Nullable String quote, @Nullable String orderId,
                     @Nullable String note) {
            this(time, type, level, price, base, quote, orderId, note, null, null);
        }

        /** Total number of errors in this event (1 for a plain ERROR event). */
        public int errorCount() {
            return errors == null ? (type == EventType.ERROR ? 1 : 0)
                    : errors.stream().mapToInt(ErrorCount::count).sum();
        }

        /** "type x count, ..." for the errors in this event; empty if none. */
        public String errorSummary() {
            return errors == null ? "" : errors.stream().map(e -> e.type() + " x" + e.count())
                    .collect(java.util.stream.Collectors.joining(", "));
        }
    }

    /** How many errors of {@code type} (operation and exception, e.g. "place sell: IOException") an ERROR event holds. */
    public record ErrorCount(String type, int count) {
    }

    /**
     * Same state with an error recorded: merged into the last history event if that's an ERROR (counted by
     * {@code errorType}), else appended as a new ERROR event.
     */
    public SellDownBotState withError(String time, @Nullable Integer level, String errorType, String message) {
        Event last = history.isEmpty() ? null : history.get(history.size() - 1);
        Event event;
        if (last != null && last.type() == EventType.ERROR) {
            List<ErrorCount> counts = new ArrayList<>(last.errors() == null ? List.of() : last.errors());
            int index = counts.stream().map(ErrorCount::type).toList().indexOf(errorType);
            if (index < 0) {
                counts.add(new ErrorCount(errorType, 1));
            } else {
                counts.set(index, new ErrorCount(errorType, counts.get(index).count() + 1));
            }
            event = new Event(last.time(), EventType.ERROR, Objects.equals(last.level(), level) ? level : null,
                    null, null, null, null, message, time, counts);
        } else {
            event = new Event(time, EventType.ERROR, level, null, null, null, null, message, time,
                    List.of(new ErrorCount(errorType, 1)));
        }
        List<Event> events = new ArrayList<>(history);
        if (last != null && last.type() == EventType.ERROR) {
            events.set(events.size() - 1, event);
        } else {
            events.add(event);
        }
        return new SellDownBotState(definition, levels, events, orders, exchangeName, manualBreakEvenTrigger,
                manualBreakEvenPrice);
    }

    /** Same state with {@code event} appended to the history. */
    public SellDownBotState withEvent(Event event) {
        List<Event> events = new ArrayList<>(history);
        events.add(event);
        return new SellDownBotState(definition, levels, events, orders, exchangeName, manualBreakEvenTrigger,
                manualBreakEvenPrice);
    }

    /** Same state with {@code level} replacing the level with the same number. */
    public SellDownBotState withLevel(Level level) {
        return new SellDownBotState(definition,
                levels.stream().map(l -> l.number() == level.number() ? level : l).toList(), history, orders, exchangeName,
                manualBreakEvenTrigger, manualBreakEvenPrice);
    }

    /** Same state with {@code order} replacing the order with the same id, or appended if new. */
    public SellDownBotState withOrder(BotOrder order) {
        List<BotOrder> updated = new ArrayList<>(orders);
        int index = updated.stream().map(BotOrder::orderId).toList().indexOf(order.orderId());
        if (index < 0) {
            updated.add(order);
        } else {
            updated.set(index, order);
        }
        return new SellDownBotState(definition, levels, history, updated, exchangeName, manualBreakEvenTrigger,
                manualBreakEvenPrice);
    }

    /** Same state bound to {@code newExchangeName}. */
    public SellDownBotState withExchangeName(String newExchangeName) {
        return new SellDownBotState(definition, levels, history, orders, newExchangeName, manualBreakEvenTrigger,
                manualBreakEvenPrice);
    }

    /** Same state with the bot-wide manual break-even trigger and its buy price set (nulls: cleared). */
    public SellDownBotState withManualBreakEven(@Nullable String trigger, @Nullable String price) {
        return new SellDownBotState(definition, levels, history, orders, exchangeName, trigger, price);
    }

    public @Nullable BotOrder order(String orderId) {
        return orders.stream().filter(o -> o.orderId().equals(orderId)).findFirst().orElse(null);
    }

    /** The level's latest order (its sell, or later its break-even buy), or null if none. */
    public @Nullable BotOrder lastOrder(int levelNumber) {
        return orders.stream().filter(o -> o.level() == levelNumber).reduce((first, second) -> second).orElse(null);
    }

    /** PLACED: order placed; UPDATED: fills changed; CANCEL_REQUESTED: bot asked to cancel; CLOSED: filled/cancelled. */
    public enum OrderEventType { PLACED, UPDATED, CANCEL_REQUESTED, CLOSED }

    /** One change of an order, with the order's fill totals and status after it. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BotOrderEvent(String time, OrderEventType type, String filled, String filledQuote, String fee,
                                BotExchange.OrderStatus status) {
    }

    /**
     * One order the bot placed for {@code level}: a SELL (the level's sell) or a BUY (its break-even). {@code price}:
     * limit price; {@code size}, {@code filled}: BASE; {@code filledQuote}, {@code fee}: QUOTE. Times: ISO-8601.
     * {@code history}: the order's changes, oldest first.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BotOrder(String orderId, int level, BotExchange.Side side, String price, String size, String filled,
                           String filledQuote, String fee, BotExchange.OrderStatus status, String placedAt,
                           @Nullable String closedAt, List<BotOrderEvent> history) {
        public BotOrder {
            history = history == null ? List.of() : List.copyOf(history);
        }

        /** The order as the exchange last reported it. */
        public BotExchange.Order toExchangeOrder() {
            return new BotExchange.Order(orderId, side, new BigDecimal(price), new BigDecimal(size),
                    new BigDecimal(filled), new BigDecimal(filledQuote), new BigDecimal(fee), status);
        }

        /** Order as placed, with a PLACED event. */
        public static BotOrder placed(int level, BotExchange.Order order, Instant time) {
            BotOrder blank = new BotOrder(order.orderId(), level, order.side(), plain(order.price()),
                    plain(order.size()), "0", "0", "0", BotExchange.OrderStatus.OPEN, time.toString(), null, List.of());
            return blank.with(order, OrderEventType.PLACED, time);
        }

        /** True if {@code order} differs from what's recorded (fills or status). */
        public boolean changedBy(BotExchange.Order order) {
            return !filled.equals(plain(order.filledSize())) || !filledQuote.equals(plain(order.filledFunds()))
                    || !fee.equals(plain(order.fee())) || status != order.status();
        }

        /** Takes {@code order}'s fills and status, and records them as a {@code type} event. */
        public BotOrder with(BotExchange.Order order, OrderEventType type, Instant time) {
            List<BotOrderEvent> events = new ArrayList<>(history);
            events.add(new BotOrderEvent(time.toString(), type, plain(order.filledSize()), plain(order.filledFunds()),
                    plain(order.fee()), order.status()));
            return new BotOrder(orderId, level, side, price, size, plain(order.filledSize()),
                    plain(order.filledFunds()), plain(order.fee()), order.status(), placedAt,
                    order.isDone() ? time.toString() : closedAt, events);
        }

        /** Records a CANCEL_REQUESTED event, fills and status unchanged. */
        public BotOrder cancelRequested(Instant time) {
            List<BotOrderEvent> events = new ArrayList<>(history);
            events.add(new BotOrderEvent(time.toString(), OrderEventType.CANCEL_REQUESTED, filled, filledQuote, fee,
                    status));
            return new BotOrder(orderId, level, side, price, size, filled, filledQuote, fee, status, placedAt,
                    closedAt, events);
        }

        @JsonIgnore
        public boolean isCancelRequested() {
            return history.stream().anyMatch(e -> e.type() == OrderEventType.CANCEL_REQUESTED);
        }

        public BigDecimal remaining() {
            return status == BotExchange.OrderStatus.OPEN
                    ? new BigDecimal(size).subtract(new BigDecimal(filled)) : BigDecimal.ZERO;
        }

        public @Nullable BigDecimal averageFillPrice() {
            BigDecimal base = new BigDecimal(filled);
            return base.signum() == 0 ? null
                    : new BigDecimal(filledQuote).divide(base, MathContext.DECIMAL128);
        }
    }

    /** BASE and QUOTE held; {@code fee}: QUOTE paid in fees, already taken out of {@code quote}. */
    public record Holdings(BigDecimal base, BigDecimal quote, BigDecimal fee) {
        /** Applies an order's fills: a sell moves BASE to QUOTE, a buy the reverse; fees come out of QUOTE. */
        Holdings with(BotOrder order) {
            return with(order, BigDecimal.ONE);
        }

        /** Applies a share (0..1) of an order's fills. */
        Holdings with(BotOrder order, BigDecimal share) {
            BigDecimal filled = new BigDecimal(order.filled()).multiply(share);
            BigDecimal filledQuote = new BigDecimal(order.filledQuote()).multiply(share);
            BigDecimal orderFee = new BigDecimal(order.fee()).multiply(share);
            boolean sell = order.side() == BotExchange.Side.SELL;
            return new Holdings(sell ? base.subtract(filled) : base.add(filled),
                    (sell ? quote.add(filledQuote) : quote.subtract(filledQuote)).subtract(orderFee),
                    fee.add(orderFee));
        }
    }

    /**
     * What the level holds: its BASE to sell, with its sells' fills applied, plus its share of a break-even buy.
     * The buy of levels N-1 and N is paid from both levels' QUOTE, so each gets a share proportional to the QUOTE it
     * held before the buy (or to its BASE to sell if they hold none).
     */
    public Holdings holdings(Level level) {
        Holdings holdings = sold(level.number());
        for (BotOrder order : orders) {
            if (order.side() == BotExchange.Side.BUY
                    && (order.level() == level.number() || order.level() - 1 == level.number())) {
                holdings = holdings.with(order, breakEvenShare(level.number(), order.level()));
            }
        }
        return holdings;
    }

    /** Level's BASE to sell with its sells' fills applied. */
    Holdings sold(int number) {
        Holdings holdings = new Holdings(new BigDecimal(plan(levels.get(number - 1)).baseToSell()), BigDecimal.ZERO,
                BigDecimal.ZERO);
        for (BotOrder order : orders) {
            if (order.level() == number && order.side() == BotExchange.Side.SELL) {
                holdings = holdings.with(order);
            }
        }
        return holdings;
    }

    BigDecimal breakEvenShare(int number, int last) {
        BigDecimal quote = sold(number).quote();
        BigDecimal pool = sold(last - 1).quote().add(sold(last).quote());
        if (quote.signum() < 0 || pool.signum() <= 0) {
            quote = new BigDecimal(plan(levels.get(number - 1)).baseToSell());
            pool = new BigDecimal(plan(levels.get(last - 2)).baseToSell())
                    .add(new BigDecimal(plan(levels.get(last - 1)).baseToSell()));
        }
        return quote.divide(pool, MathContext.DECIMAL64);
    }

    /** What the whole position holds: the definition's BASE and QUOTE, with every order's fills applied. */
    public Holdings holdings() {
        Holdings holdings = new Holdings(new BigDecimal(definition.base()), new BigDecimal(definition.quote()),
                BigDecimal.ZERO);
        for (BotOrder order : orders) {
            holdings = holdings.with(order);
        }
        return holdings;
    }

    /** True once a level broke even, or all levels are sold. */
    @JsonIgnore
    public boolean isFinished() {
        return levels.stream().anyMatch(l -> l.status() == Status.BROKEN_EVEN)
                || levels.stream().allMatch(l -> l.status() == Status.SOLD);
    }

    /**
     * Break-even of the latest SOLD level N > 1: spend all the QUOTE levels N-1 and N actually received
     * ({@code quote}, after fees) on BASE; at the planned price that buys back all the BASE they sold ({@code base}),
     * below it more (gain), above it less (loss). {@code autoPrice}, {@code autoTrigger}: level N's planned break-even
     * price and trigger, from the definition. {@code autoUnavailable}: why the bot won't break even automatically
     * (it would buy back less BASE than sold, or collide with the next sell), else null. {@code manualTrigger}: the bot-wide one set by
     * hand (e.g. the current price), overrides the automatic one; {@code manualPrice}: its buy price, the trigger plus the
     * fill buffer, like the planned ones. {@code price}, {@code trigger}: what the bot uses (manual, else automatic if
     * available), else null.
     */
    public record BreakEven(int level, BigDecimal base, BigDecimal quote, BigDecimal autoPrice, BigDecimal autoTrigger,
                            @Nullable String autoUnavailable, @Nullable BigDecimal manualTrigger,
                            @Nullable BigDecimal manualPrice,
                            @Nullable BigDecimal price, @Nullable BigDecimal trigger) {
    }

    /**
     * Break-even of the latest SOLD level (or the one BUYING_BACK / BROKEN_EVEN), or null if there is none or it's
     * level 1. {@code base} and {@code quote} are net of what break-even buys already filled.
     */
    public @Nullable BreakEven breakEven() {
        Level latest = levels.stream().filter(l -> l.status() == Status.SOLD || l.status() == Status.BUYING_BACK
                || l.status() == Status.BROKEN_EVEN).reduce((first, second) -> second).orElse(null);
        if (latest == null || latest.number() < 2) {
            return null;
        }
        int number = latest.number();
        LevelPlan plan = plan(latest);
        if (plan.breakEvenPrice() == null || plan.breakEvenTriggerPrice() == null) {
            return null;
        }
        Holdings bought = new Holdings(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        for (BotOrder order : orders) {
            if (order.level() == number && order.side() == BotExchange.Side.BUY) {
                bought = bought.with(order);
            }
        }
        BigDecimal base = soldBase(number - 1).add(soldBase(number)).subtract(bought.base());
        BigDecimal quote = sold(number - 1).quote().add(sold(number).quote()).add(bought.quote());
        BigDecimal autoPrice = new BigDecimal(plan.breakEvenPrice());
        BigDecimal autoTrigger = new BigDecimal(plan.breakEvenTriggerPrice());
        BigDecimal autoGain = size(quote, autoPrice).subtract(base);
        String unavailable = null;
        if (autoGain.signum() < 0) {
            unavailable = "would buy back " + plain(autoGain.negate().setScale(6, java.math.RoundingMode.HALF_UP))
                    + " BASE less than sold at the planned price " + plain(autoPrice);
        } else if (number < levels.size()) {
            Level next = levels.get(number);
            BigDecimal nextTrigger = new BigDecimal(plan(next).sellTriggerPrice());
            if ((next.status() == Status.PENDING || next.status() == Status.SELLING)
                    && autoTrigger.compareTo(nextTrigger) <= 0) {
                unavailable = "its trigger " + plain(autoTrigger.setScale(6, java.math.RoundingMode.HALF_UP))
                        + " is at or below level " + next.number() + "'s sell trigger " + plain(nextTrigger);
            }
        }
        BigDecimal manualTrigger = manualBreakEvenTrigger == null ? null : new BigDecimal(manualBreakEvenTrigger);
        BigDecimal manual = manualTrigger == null || manualBreakEvenPrice == null ? null
                : new BigDecimal(manualBreakEvenPrice);
        if (manual == null) {
            manualTrigger = null;
        }
        BigDecimal price = manual != null ? manual : unavailable == null ? autoPrice : null;
        BigDecimal trigger = manualTrigger != null ? manualTrigger : unavailable == null ? autoTrigger : null;
        return new BreakEven(number, base, quote, autoPrice, autoTrigger, unavailable, manualTrigger, manual, price,
                trigger);
    }

    /** Break-even buy price for a trigger: trigger / (1 - fill buffer), as the planner does (trigger = price x (1 - buffer)). */
    public BigDecimal breakEvenPriceForTrigger(BigDecimal trigger) {
        BigDecimal buffer = BigDecimal.ONE.subtract(new BigDecimal(definition.fillBuffer()).movePointLeft(2));
        return trigger.divide(buffer, 10, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
    }

    /** BASE the break-even buys at {@code price}: all its QUOTE, maker fee on top. */
    public BigDecimal breakEvenSize(BreakEven breakEven, BigDecimal price) {
        return size(breakEven.quote(), price);
    }

    /** BASE bought back beyond what was sold, at {@code price}; negative: loss (less BASE back than sold). */
    public BigDecimal breakEvenGain(BreakEven breakEven, BigDecimal price) {
        return breakEvenSize(breakEven, price).subtract(breakEven.base());
    }

    /** Debt ratio of the whole position right after the break-even buy at {@code price}. */
    public @Nullable BigDecimal debtRatioAfterBreakEven(BreakEven breakEven, BigDecimal price) {
        Holdings now = holdings();
        BigDecimal size = breakEvenSize(breakEven, price);
        return com.poplavok.data.utils.LiquidationCalculator.debtRatio(new BigDecimal(definition.debt()), true,
                now.base().add(size), now.quote().subtract(size.multiply(price).multiply(BigDecimal.ONE.add(makerFee()))),
                price);
    }

    /** BASE that {@code quote} buys at {@code price} with the maker fee on top, rounded down. */
    BigDecimal size(BigDecimal quote, BigDecimal price) {
        return quote.signum() <= 0 ? BigDecimal.ZERO : quote.divide(price.multiply(BigDecimal.ONE.add(makerFee())), 10,
                java.math.RoundingMode.FLOOR).stripTrailingZeros();
    }

    BigDecimal makerFee() {
        return new BigDecimal(definition.makerFee()).movePointLeft(2);
    }

    /** BASE the level's sells actually filled. */
    BigDecimal soldBase(int number) {
        return new BigDecimal(plan(levels.get(number - 1)).baseToSell()).subtract(sold(number).base());
    }

    /**
     * State of definition level {@code number}. {@code orderId}: the open order (SELLING or BUYING_BACK), else null.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Level(int number, Status status, @Nullable String orderId) {
        public Level withStatus(Status newStatus, @Nullable String newOrderId) {
            return new Level(number, newStatus, newOrderId);
        }
    }

    public static SellDownBotState of(String symbol, SellDownPlanner.Params params, SellDownPlanner.Plan plan,
                                      Instant createdAt) {
        Definition definition = new Definition(symbol, createdAt.toString(), plain(params.price()),
                plain(params.debt()), plain(params.base()), plain(params.quote()), percent(params.startRatio()),
                percent(params.endRatio()), percent(params.liquidationRatio()), percent(params.fillBuffer()),
                percent(params.slippage()), percent(params.makerFee()), percent(params.takerFee()),
                plan.levels().stream().map(level -> new LevelPlan(level.number(), percent(level.targetRatio()),
                        plain(level.sellPrice()), plain(level.sellTriggerPrice()),
                        plain(level.baseSold()), plain(level.quoteReceived()), plainOrNull(level.breakEvenPrice()),
                        plainOrNull(level.breakEvenTriggerPrice()))).toList());
        return new SellDownBotState(definition,
                plan.levels().stream().map(level -> new Level(level.number(), Status.PENDING, null)).toList(),
                List.of(new Event(createdAt.toString(), EventType.CREATED, null, plain(params.price()), null, null,
                        null, plan.levels().size() + " level(s)")), List.of(), null, null, null);
    }

    static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    static @Nullable String plainOrNull(@Nullable BigDecimal value) {
        return value == null ? null : plain(value);
    }

    static String percent(BigDecimal fraction) {
        return plain(fraction.movePointRight(2));
    }

    public void write(File file) throws IOException {
        MAPPER.writeValue(file, this);
    }

    public static SellDownBotState read(File file) throws IOException {
        return MAPPER.readValue(file, SellDownBotState.class);
    }
}
