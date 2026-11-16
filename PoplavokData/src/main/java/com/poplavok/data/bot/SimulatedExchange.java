package com.poplavok.data.bot;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

/**
 * Exchange operated by hand: the user sets the price and fills or cancels orders, partially or fully, at the order's
 * limit price or better (higher for a sell, lower for a buy). Fees are {@code feeRate} (fraction) of the filled QUOTE. Orders never fill by themselves.
 * Order polls ({@link #getOrder}) are logged, newest first, up to {@link #MAX_POLLS}.
 * Errors can be injected: while a {@link Failure} is set, every such bot call throws an {@link IOException}.
 * Listeners are called on the thread making the change, outside the exchange's lock.
 */
public class SimulatedExchange implements BotExchange {
    public static final int MAX_POLLS = 50;
    public static final String NAME = "SIMULATED";

    /** One {@link #getOrder} call and the order it returned; {@code failed}: an injected error was thrown instead. */
    public record Poll(Instant time, Order order, boolean failed) {
    }

    /** Bot calls that can be made to fail. */
    public enum Failure { PLACE, POLL, CANCEL }

    private final BigDecimal feeRate;
    private final Clock clock;
    private final Deque<Poll> polls = new ArrayDeque<>();
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final List<PriceListener> priceListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
    private final Set<Failure> failures = EnumSet.noneOf(Failure.class);
    private @Nullable BigDecimal price;
    private int nextId = 1;

    public SimulatedExchange(BigDecimal feeRate) {
        this(feeRate, Clock.systemUTC());
    }

    public SimulatedExchange(BigDecimal feeRate, Clock clock) {
        this.feeRate = feeRate;
        this.clock = clock;
    }

    /** Called after any change: price set, order placed, filled or cancelled, order polled. */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    /** While {@code failing}, every {@code failure} call throws an {@link IOException}. */
    public synchronized void setFailing(Failure failure, boolean failing) {
        if (failing) {
            failures.add(failure);
        } else {
            failures.remove(failure);
        }
    }

    public synchronized boolean isFailing(Failure failure) {
        return failures.contains(failure);
    }

    public synchronized @Nullable BigDecimal price() {
        return price;
    }

    /** Latest order polls, newest first. */
    public synchronized List<Poll> polls() {
        return new ArrayList<>(polls);
    }

    /** All orders, oldest first. */
    public synchronized List<Order> orders() {
        return new ArrayList<>(orders.values());
    }

    public void setPrice(BigDecimal newPrice) {
        if (newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        synchronized (this) {
            price = newPrice;
        }
        priceListeners.forEach(listener -> listener.onPrice(newPrice));
        changed();
    }

    /** Fills {@code size} more of an open order (capped at what remains) at its limit price. */
    public void fill(String orderId, BigDecimal size) {
        fill(orderId, size, null);
    }

    /**
     * Fills {@code size} more of an open order (capped at what remains) at {@code fillPrice}, which must be the
     * limit price or better; null: the limit price.
     */
    public void fill(String orderId, BigDecimal size, @Nullable BigDecimal fillPrice) {
        if (size.signum() <= 0) {
            throw new IllegalArgumentException("Fill size must be positive");
        }
        synchronized (this) {
            Order order = openOrder(orderId);
            BigDecimal at = fillPrice == null ? order.price() : fillPrice;
            int vsLimit = at.compareTo(order.price());
            if (at.signum() <= 0 || (order.side() == Side.SELL ? vsLimit < 0 : vsLimit > 0)) {
                throw new IllegalArgumentException("Fill price must be " + (order.side() == Side.SELL
                        ? "at or above" : "at or below") + " the limit price " + order.price().toPlainString());
            }
            BigDecimal filled = size.min(order.remainingSize());
            BigDecimal funds = filled.multiply(at, MathContext.DECIMAL128);
            BigDecimal filledSize = order.filledSize().add(filled);
            orders.put(orderId, new Order(orderId, order.side(), order.price(), order.size(), filledSize,
                    order.filledFunds().add(funds), order.fee().add(funds.multiply(feeRate, MathContext.DECIMAL128)),
                    filledSize.compareTo(order.size()) >= 0 ? OrderStatus.FILLED : OrderStatus.OPEN));
        }
        changed();
    }

    @Override
    public String exchangeName() {
        return NAME;
    }

    /** Puts back an order as it was (e.g. from a reloaded bot); new ids continue after it. */
    public void restore(Order order) {
        synchronized (this) {
            orders.put(order.orderId(), order);
            if (order.orderId().startsWith("sim-")) {
                try {
                    nextId = Math.max(nextId, Integer.parseInt(order.orderId().substring(4)) + 1);
                } catch (NumberFormatException ignored) {
                    // not one of ours; keep numbering
                }
            }
        }
        changed();
    }

    @Override
    public Order placeLimitOrder(String symbol, Side side, BigDecimal limitPrice, BigDecimal size)
            throws IOException {
        if (isFailing(Failure.PLACE)) {
            throw injected(Failure.PLACE);
        }
        if (limitPrice.signum() <= 0 || size.signum() <= 0) {
            throw new IllegalArgumentException("Price and size must be positive");
        }
        Order order;
        synchronized (this) {
            String id = "sim-" + nextId++;
            order = new Order(id, side, limitPrice, size, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    OrderStatus.OPEN);
            orders.put(id, order);
        }
        changed();
        return order;
    }

    @Override
    public Order getOrder(String symbol, String orderId) throws IOException {
        Order order;
        boolean failed;
        synchronized (this) {
            order = find(orderId);
            failed = failures.contains(Failure.POLL);
            polls.addFirst(new Poll(clock.instant(), order, failed));
            while (polls.size() > MAX_POLLS) {
                polls.removeLast();
            }
        }
        changed();
        if (failed) {
            throw injected(Failure.POLL);
        }
        return order;
    }

    /** The bot's cancel: fails while {@link Failure#CANCEL} is set, else {@link #cancel}. */
    @Override
    public void cancelOrder(String symbol, String orderId) throws IOException {
        if (isFailing(Failure.CANCEL)) {
            throw injected(Failure.CANCEL);
        }
        cancel(orderId);
    }

    /** Cancel by the exchange (never injected): cancels an open order; done orders are left as they are. */
    public void cancel(String orderId) {
        synchronized (this) {
            Order order = find(orderId);
            if (order.isDone()) {
                return;
            }
            orders.put(orderId, new Order(orderId, order.side(), order.price(), order.size(), order.filledSize(),
                    order.filledFunds(), order.fee(), OrderStatus.CANCELLED));
        }
        changed();
    }

    @Override
    public Subscription subscribePrice(String symbol, PriceListener listener) {
        priceListeners.add(listener);
        return () -> priceListeners.remove(listener);
    }

    private static IOException injected(Failure failure) {
        return new IOException("Injected " + failure.name().toLowerCase(java.util.Locale.ROOT) + " failure");
    }

    private Order find(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("No order " + orderId);
        }
        return order;
    }

    private Order openOrder(String orderId) {
        Order order = find(orderId);
        if (order.isDone()) {
            throw new IllegalStateException("Order " + orderId + " is " + order.status());
        }
        return order;
    }

    private void changed() {
        changeListeners.forEach(Runnable::run);
    }
}
