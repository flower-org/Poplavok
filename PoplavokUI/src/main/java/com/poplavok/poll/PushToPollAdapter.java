package com.poplavok.poll;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Single-channel push-to-long-poll converter: feed values in via {@link #accept} (e.g. from any
 * listener callback), read them via {@link #poll}.
 *
 * <p>{@link #poll} completes immediately with every buffered update newer than {@code lastUpdateNo};
 * if there are none, the future stays pending and completes as soon as the next update arrives.
 * Only the last {@code capacity} updates are kept, so a client that fell behind sees a gap in
 * {@link Update#updateNo()}. {@code null} values are ignored.
 *
 * <p>Updates are numbered {@code startAfter + 1}, {@code startAfter + 2}, ... (1, 2, ... by default) without gaps and kept in a ring buffer, so finding where a poll
 * starts is O(1) and a poll costs only the number of updates it returns.
 *
 * <p>Subscription lifecycle is the caller's concern; see {@link KeyedPushToPollAdapter} for a keyed
 * variant that subscribes on demand and evicts idle keys.
 */
public final class PushToPollAdapter<V> implements Consumer<V>, AutoCloseable {
    public record Update<V>(long updateNo, V value) {}

    record Waiter<V>(long lastUpdateNo, CompletableFuture<List<Update<V>>> future) {}

    record Completion<V>(CompletableFuture<List<Update<V>>> future, List<Update<V>> updates) {}

    /**
     * Guards all state below. Package-private so {@link KeyedPushToPollAdapter} can guard its per-key
     * state with the same lock; holders must not call {@link #accept} or {@link #close} if the futures
     * they complete must run outside it.
     */
    final ReentrantLock lock = new ReentrantLock();
    /**
     * Holds the last {@code min(lastUpdateNo - startAfter, length)} updates. Update numbers have no gaps,
     * so update {@code n} always sits in slot {@code (n - 1) % length}; no head or size needs tracking.
     */
    private final Update<V>[] ringBuffer;
    private final long startAfter;
    private long lastUpdateNo;
    /** Insertion-ordered for fairness; a set so a cancelled waiter can be removed in O(1). */
    private final Set<Waiter<V>> waiters = new LinkedHashSet<>();
    private boolean closed = false;

    public PushToPollAdapter(int capacity) {
        this(capacity, 0);
    }

    /** The first update is numbered {@code startAfter + 1}. */
    @SuppressWarnings("unchecked")
    public PushToPollAdapter(int capacity, long startAfter) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        if (startAfter < 0) {
            throw new IllegalArgumentException("startAfter must not be negative: " + startAfter);
        }
        this.ringBuffer = (Update<V>[]) new Update<?>[capacity];
        this.startAfter = startAfter;
        this.lastUpdateNo = startAfter;
    }

    @Override
    public void accept(@Nullable V value) {
        if (value == null) {
            return;
        }
        List<Completion<V>> completions = new ArrayList<>();
        lock.lock();
        try {
            if (closed) {
                return;
            }
            lastUpdateNo++;
            ringBuffer[slot(lastUpdateNo)] = new Update<>(lastUpdateNo, value);
            // A waiter whose lastUpdateNo is ahead of everything buffered keeps waiting.
            Iterator<Waiter<V>> it = waiters.iterator();
            while (it.hasNext()) {
                Waiter<V> waiter = it.next();
                if (waiter.future().isDone()) {
                    it.remove();
                } else if (waiter.lastUpdateNo() < lastUpdateNo) {
                    completions.add(new Completion<>(waiter.future(), updatesAfter(waiter.lastUpdateNo())));
                    it.remove();
                }
            }
        } finally {
            lock.unlock();
        }
        // Completed outside the lock so client callbacks never run under it.
        completions.forEach(completion -> completion.future().complete(completion.updates()));
    }

    /**
     * Returns the buffered updates with {@code updateNo > lastUpdateNo}, or, if there are none yet, a
     * future that completes with the next update(s). Pass {@code 0} to get everything buffered.
     * After {@link #close()}, completes with an empty list.
     */
    public CompletableFuture<List<Update<V>>> poll(long lastUpdate) {
        lock.lock();
        try {
            if (closed) {
                return CompletableFuture.completedFuture(List.of());
            }
            List<Update<V>> available = updatesAfter(lastUpdate);
            if (!available.isEmpty()) {
                return CompletableFuture.completedFuture(available);
            }
            CompletableFuture<List<Update<V>>> future = new CompletableFuture<>();
            Waiter<V> waiter = new Waiter<>(lastUpdate, future);
            waiters.add(waiter);
            // We only ever complete normally, so exceptional completion means the client cancelled or
            // timed out: drop the waiter now instead of keeping it until the next value.
            CompletableFuture<?> unused = future.whenComplete((updates, error) -> {
                if (error != null) {
                    removeWaiter(waiter);
                }
            });
            return future;
        } finally {
            lock.unlock();
        }
    }

    /** Drops the buffer, ignores further values and completes pending polls with an empty list. */
    @Override
    public void close() {
        List<Waiter<V>> pending;
        lock.lock();
        try {
            closed = true;
            pending = new ArrayList<>(waiters);
            waiters.clear();
            Arrays.fill(ringBuffer, null);
        } finally {
            lock.unlock();
        }
        pending.forEach(waiter -> waiter.future().complete(List.of()));
    }

    /** O(1) to locate, O(k) to copy the k newer updates. Call with {@link #lock} held. */
    private List<Update<V>> updatesAfter(long lastUpdate) {
        if (lastUpdate >= lastUpdateNo) {
            return List.of();
        }
        long firstBufferedNo = Math.max(startAfter + 1, lastUpdateNo - ringBuffer.length + 1);
        // Compared before adding 1, so an extreme lastUpdate can't overflow.
        long from = lastUpdate < firstBufferedNo ? firstBufferedNo : lastUpdate + 1;
        List<Update<V>> result = new ArrayList<>((int) (lastUpdateNo - from + 1));
        for (long no = from; no <= lastUpdateNo; no++) {
            result.add(ringBuffer[slot(no)]);
        }
        return result;
    }

    /** Number of the newest update, or {@code startAfter} if none; final once {@link #close()} returns. */
    long lastUpdateNo() {
        lock.lock();
        try {
            return lastUpdateNo;
        } finally {
            lock.unlock();
        }
    }

    int waiterCount() {
        lock.lock();
        try {
            return waiters.size();
        } finally {
            lock.unlock();
        }
    }

    private void removeWaiter(Waiter<V> waiter) {
        lock.lock();
        try {
            waiters.remove(waiter);
        } finally {
            lock.unlock();
        }
    }

    private int slot(long updateNo) {
        return (int) ((updateNo - 1) % ringBuffer.length);
    }
}
