package com.poplavok.forms;

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
 */
public record SellDownBotState(Definition definition, List<Level> levels, List<Event> history, List<BotOrder> orders,
                               @Nullable String exchangeName) {
    public static final String EXTENSION = "*.bot";
    static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public SellDownBotState {
        Objects.requireNonNull(definition, "definition");
        levels = levels == null ? List.of() : List.copyOf(levels);
        history = history == null ? List.of() : List.copyOf(history);
        orders = orders == null ? List.of() : List.copyOf(orders);
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
     * One planned sell level: sell {@code baseToSell} at {@code sellPrice}, starting at {@code sellTriggerPrice}
     * ({@code immediate}: at market right away). Break-even prices are null for level 1.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LevelPlan(int number, String targetRatio, String sellPrice, String sellTriggerPrice,
                            boolean immediate, String baseToSell, String expectedQuote,
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
        CREATED, BOUND, ORDER_PLACED, ORDER_FILLED, ORDER_CANCELLED, SOLD, BROKEN_EVEN, FINISHED, ERROR
    }

    /**
     * One bot action. {@code time}: ISO-8601 instant. {@code level}: level number, null if not about a level.
     * {@code price}, {@code base}, {@code quote}: what was ordered/filled, where relevant. {@code orderId}: exchange
     * order, where relevant. {@code note}: free text, e.g. an error message.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(String time, EventType type, @Nullable Integer level, @Nullable String price,
                        @Nullable String base, @Nullable String quote, @Nullable String orderId,
                        @Nullable String note) {
    }

    /** Same state with {@code event} appended to the history. */
    public SellDownBotState withEvent(Event event) {
        List<Event> events = new ArrayList<>(history);
        events.add(event);
        return new SellDownBotState(definition, levels, events, orders, exchangeName);
    }

    /** Same state with {@code level} replacing the level with the same number. */
    public SellDownBotState withLevel(Level level) {
        return new SellDownBotState(definition,
                levels.stream().map(l -> l.number() == level.number() ? level : l).toList(), history, orders, exchangeName);
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
        return new SellDownBotState(definition, levels, history, updated, exchangeName);
    }

    /** Same state bound to {@code newExchangeName}. */
    public SellDownBotState withExchangeName(String newExchangeName) {
        return new SellDownBotState(definition, levels, history, orders, newExchangeName);
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
                        plain(level.sellPrice()), plain(level.sellTriggerPrice()), level.immediate(),
                        plain(level.baseSold()), plain(level.quoteReceived()), plainOrNull(level.breakEvenPrice()),
                        plainOrNull(level.breakEvenTriggerPrice()))).toList());
        return new SellDownBotState(definition,
                plan.levels().stream().map(level -> new Level(level.number(), Status.PENDING, null)).toList(),
                List.of(new Event(createdAt.toString(), EventType.CREATED, null, plain(params.price()), null, null,
                        null, plan.levels().size() + " level(s)")), List.of(), null);
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
