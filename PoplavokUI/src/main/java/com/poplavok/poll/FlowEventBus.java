package com.poplavok.poll;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Multiplexes many futures into one wait, like {@code select}/{@code epoll}: register futures of any type
 * (e.g. {@link PushToPollAdapter#poll} results), then {@link #next()} returns a future that completes with
 * the first of them to fire.
 *
 * <p>A registered future "fires" when it completes in any way (normally, exceptionally or cancelled).
 * It is then delivered once, identified by the future instance itself, and is no longer registered: the
 * consumer re-registers a fresh future (e.g. the next poll) when it wants more.
 *
 * <p>Fired futures are delivered in firing order. Results are completed outside the bus lock, so
 * consumer callbacks may freely call back into the bus.
 *
 * <p>The bus serves a single consumer: at most one {@code next()} result may be pending at a time, and
 * calling {@code next()} while one is pending throws {@link IllegalStateException}. This fails fast on an
 * accidental second consumer; handing out the pending result again would instead let its holders silently
 * share cancellation and each process the same fired future. A new call is allowed once the pending result
 * has completed or been cancelled; cancelling loses no events, as anything that fires stays queued for the
 * next call.
 *
 * <p>The bus owns no resources: to stop, cancel the pending result (if any) and drop the bus.
 *
 * <p>Similar designs that multiplex futures:
 * <ul>
 *   <li>Rust {@code futures::stream::FuturesUnordered}: futures pushed dynamically, {@code next()} yields
 *       them in completion order;</li>
 *   <li>Rust Tokio {@code JoinSet}: tasks added dynamically, {@code join_next()} yields them in completion
 *       order.</li>
 * </ul>
 */
public final class FlowEventBus {
    private final ReentrantLock lock = new ReentrantLock();
    private final Set<CompletableFuture<?>> registered = new HashSet<>();
    /** Fired but not yet delivered, in firing order; never holds duplicates, as firing unregisters. */
    private final Deque<CompletableFuture<?>> fired = new ArrayDeque<>();
    @Nullable private CompletableFuture<CompletableFuture<?>> pending;

    /**
     * Watches {@code future}; it is delivered by {@link #next()} once it fires (immediately if it's
     * already done). Registering an already registered future has no effect.
     */
    public void register(CompletableFuture<?> future) {
        lock.lock();
        try {
            if (!registered.add(future)) {
                return;
            }
        } finally {
            lock.unlock();
        }
        // Outside the lock: runs synchronously if the future is already done.
        CompletableFuture<?> unused = future.whenComplete((result, error) -> onFired(future));
    }

    /**
     * Stops watching {@code future}, also dropping it if it fired but wasn't delivered yet. Returns
     * whether it was registered or awaiting delivery.
     *
     * <p>{@code false} also covers a future that is being delivered right now (taken off the queue, with
     * the pending result not yet completed); it may still be delivered. In the rare case where that delivery
     * fails because the consumer cancelled the pending result concurrently, the future is requeued
     * despite this call and delivered by a later {@code next()}.
     */
    public boolean unregister(CompletableFuture<?> future) {
        lock.lock();
        try {
            boolean wasRegistered = registered.remove(future);
            boolean wasFired = fired.remove(future);
            return wasRegistered || wasFired;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Completes with the earliest fired future not yet delivered.
     *
     * @throws IllegalStateException if a {@code next()} result is still pending
     */
    public CompletableFuture<CompletableFuture<?>> next() {
        CompletableFuture<CompletableFuture<?>> result = new CompletableFuture<>();
        lock.lock();
        try {
            if (pending != null && !pending.isDone()) {
                throw new IllegalStateException("A next() result is already pending");
            }
            pending = result;
        } finally {
            lock.unlock();
        }
        dispatch();
        return result;
    }

    private void onFired(CompletableFuture<?> future) {
        lock.lock();
        try {
            // Unregistered meanwhile: not ours to deliver.
            if (!registered.remove(future)) {
                return;
            }
            fired.addLast(future);
        } finally {
            lock.unlock();
        }
        dispatch();
    }

    /**
     * If a result is pending and something has fired, hands the earliest fired future to it. Completes
     * outside the lock; if the consumer cancelled the result meanwhile, the future goes back to the front
     * of the queue for the next call. Doesn't check for an {@link #unregister} that raced with the failed
     * delivery (see its Javadoc).
     */
    private void dispatch() {
        while (true) {
            CompletableFuture<CompletableFuture<?>> target;
            CompletableFuture<?> first;
            lock.lock();
            try {
                if (pending == null || fired.isEmpty()) {
                    return;
                }
                target = pending;
                pending = null;
                first = fired.removeFirst();
            } finally {
                lock.unlock();
            }
            if (target.complete(first)) {
                return;
            }
            lock.lock();
            try {
                fired.addFirst(first);
            } finally {
                lock.unlock();
            }
            // Loop: a new next() may have been parked while this one was out for delivery.
        }
    }
}
