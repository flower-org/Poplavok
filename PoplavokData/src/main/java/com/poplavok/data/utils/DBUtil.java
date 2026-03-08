package com.poplavok.data.utils;

import com.google.common.util.concurrent.ListenableFuture;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class DBUtil {

    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);
    private static final int POOL_SIZE = 4;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(POOL_SIZE);

    private DBUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Executes a Runnable task asynchronously using the DB scheduler.
     *
     * @param task the task to execute
     * @return a CompletableFuture representing the task execution
     */
    public static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, scheduler)
                .exceptionally(ex -> {
                    logger.error("Error executing DB task", ex);
                    throw new RuntimeException(ex);
                });
    }

    /**
     * Executes a Supplier task asynchronously using the DB scheduler and returns the result.
     *
     * @param supplier the task to execute
     * @param <T>      the type of the result
     * @return a CompletableFuture containing the result
     */
    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, scheduler)
                .exceptionally(ex -> {
                    logger.error("Error executing DB task", ex);
                    throw new RuntimeException(ex);
                });
    }

    /**
     * Executes a Supplier task asynchronously using the DB scheduler and returns the result, blocking until complete.
     * Equivalent to connectGetResultAndClose.
     *
     * @param task the task to execute
     * @param <T>  the type of the result
     * @return the result
     */
    public static <T> T executeSync(Supplier<T> task) {
        try {
            return supplyAsync(task).get();
        } catch (Exception e) {
            Throwable cause = e;
            if (e instanceof java.util.concurrent.ExecutionException) {
                cause = e.getCause();
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    /**
     * Schedules a Runnable task to run after a delay.
     *
     * @param task  the task to execute
     * @param delay the time from now to delay execution
     * @param unit  the time unit of the delay parameter
     * @return a ScheduledFuture representing pending completion of the task
     */
    public static java.util.concurrent.ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(() -> {
            try {
                task.run();
            } catch (Exception ex) {
                logger.error("Error executing scheduled DB task", ex);
                throw new RuntimeException(ex);
            }
        }, delay, unit);
    }

    /**
     * Executes a Supplier task asynchronously using the DB scheduler and waits for the result.
     * This mimics the behavior of connecting, getting a result, and closing the resource,
     * but delegates the session management to the Supplier (e.g. the DAO method).
     *
     * @param fn the task to execute
     * @param <T>      the type of the result
     * @return the result
     */
    public static <T> T connectGetResultAndClose(Function<SessionFactory, T> fn) {
        try {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            return supplyAsync(() -> fn.apply(sessionFactory)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for DB task", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    public static <T> T connectCommitAndClose(Function<SessionFactory, T> fn) {
        try {
            SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
            return supplyAsync(() -> fn.apply(sessionFactory)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for DB task", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    /**
     * Shuts down the scheduler.
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
