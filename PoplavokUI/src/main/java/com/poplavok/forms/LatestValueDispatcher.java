package com.poplavok.forms;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Hands values to {@code consumer} on {@code executor}, coalescing bursts: while a delivery is
 * pending, newer values replace the pending one, so at most one task is queued and the consumer
 * only sees the latest value.
 *
 * <p>If {@code executor} rejects the task, the exception propagates to the {@link #submit} caller
 * and further values are dropped (the executor is assumed to be shut down).
 */
final class LatestValueDispatcher<T> {
    private final AtomicReference<T> pending = new AtomicReference<>();
    private final Executor executor;
    private final Consumer<T> consumer;

    LatestValueDispatcher(Executor executor, Consumer<T> consumer) {
        this.executor = executor;
        this.consumer = consumer;
    }

    void submit(T value) {
        if (pending.getAndSet(value) == null) {
            executor.execute(() -> {
                T latest = pending.getAndSet(null);
                if (latest != null) {
                    consumer.accept(latest);
                }
            });
        }
    }
}
