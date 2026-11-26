package com.poplavok.bots.selldown;

import com.flower.eventbus.PushToPollAdapter;
import com.poplavok.data.bot.BotExchange;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * {@link OrderStatuses} by polling: fetches every watched order with {@link BotExchange#getOrder} on the scheduler,
 * right away and then {@code pollMillis} after each round, or on {@link #pollNow()}. Every round publishes a
 * {@link Snapshot}, even with nothing watched. Rounds never overlap.
 */
final class OrderStatusPoller implements OrderStatuses, AutoCloseable {
    /** Snapshots kept for a slow reader; it reads them all in order, and a newer one supersedes an older. */
    private static final int CAPACITY = 16;

    private final BotExchange exchange;
    private final String symbol;
    private final ScheduledExecutorService scheduler;
    private final PushToPollAdapter<Snapshot> snapshots = new PushToPollAdapter<>(CAPACITY);
    /** Serializes rounds. */
    private final Object roundLock = new Object();
    /** Guarded by {@code this}. */
    private Set<String> watched = Set.of();
    /** Guarded by {@code this}. */
    private @Nullable ScheduledFuture<?> schedule;
    private volatile boolean closed;

    OrderStatusPoller(BotExchange exchange, String symbol, ScheduledExecutorService scheduler) {
        this.exchange = exchange;
        this.symbol = symbol;
        this.scheduler = scheduler;
    }

    /** Polls right away, then {@code pollMillis} after each round has ended. */
    synchronized void start(long pollMillis) {
        if (closed || schedule != null) {
            throw new IllegalStateException(closed ? "closed" : "already started");
        }
        schedule = scheduler.scheduleWithFixedDelay(this::round, 0, pollMillis, TimeUnit.MILLISECONDS);
    }

    /** Polls once more as soon as possible, besides the periodic rounds. */
    void pollNow() {
        if (!closed) {
            scheduler.execute(this::round);
        }
    }

    @Override
    public synchronized void watch(Set<String> orderIds) {
        watched = Set.copyOf(orderIds);
    }

    @Override
    public CompletableFuture<List<Update<Snapshot>>> poll(long lastUpdate) {
        return snapshots.poll(lastUpdate);
    }

    /** Stops polling and closes the stream; a round in progress publishes nothing. Idempotent. */
    @Override
    public void close() {
        closed = true;
        synchronized (this) {
            if (schedule != null) {
                schedule.cancel(false);
            }
        }
        snapshots.close();
    }

    private void round() {
        synchronized (roundLock) {
            if (closed) {
                return;
            }
            Set<String> ids;
            synchronized (this) {
                ids = watched;
            }
            List<BotExchange.Order> orders = new ArrayList<>();
            Map<String, Exception> failures = new LinkedHashMap<>();
            for (String id : ids) {
                try {
                    orders.add(exchange.getOrder(symbol, id));
                } catch (IOException | RuntimeException e) {
                    failures.put(id, e);
                }
            }
            synchronized (this) {
                Set<String> remaining = new LinkedHashSet<>(watched);
                orders.stream().filter(BotExchange.Order::isDone).forEach(o -> remaining.remove(o.orderId()));
                watched = Set.copyOf(remaining);
            }
            snapshots.accept(new Snapshot(List.copyOf(orders), Collections.unmodifiableMap(failures)));
        }
    }
}
