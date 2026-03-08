package com.poplavok.data.utils;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DBUtil {

    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);
    private static final int POOL_SIZE = 4;

    @Nullable
    private static ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(POOL_SIZE);

    public static synchronized ScheduledExecutorService scheduler() {
        if (SCHEDULER == null) {
            SCHEDULER = Executors.newScheduledThreadPool(POOL_SIZE);
        }
        return SCHEDULER;
    }

    /**
     * Shuts down the scheduler.
     */
    public static synchronized void shutdown() {
        if (SCHEDULER != null) {
            SCHEDULER.shutdown();
            try {
                if (!SCHEDULER.awaitTermination(60, TimeUnit.SECONDS)) {
                    SCHEDULER.shutdownNow();
                }
            } catch (InterruptedException e) {
                SCHEDULER.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

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
        return CompletableFuture.runAsync(task, scheduler())
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
        return CompletableFuture.supplyAsync(supplier, scheduler())
                .exceptionally(ex -> {
                    logger.error("Error executing DB task", ex);
                    throw new RuntimeException(ex);
                });
    }

    /**
     * Executes a void task asynchronously using the DB scheduler.
     * Similar to supplyAsync but for void methods.
     *
     * @param action the void action to execute
     * @return a CompletableFuture representing the task execution
     */
    public static CompletableFuture<Void> executeAsync(Runnable action) {
        return CompletableFuture.runAsync(action, scheduler())
                .exceptionally(ex -> {
                    logger.error("Error executing DB task", ex);
                    throw new RuntimeException(ex);
                });
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
        return scheduler().schedule(() -> {
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
    public static <T> T connectGetResultAndClose(Function<Session, T> fn) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Session session = sessionFactory.openSession();
        try (session) {
            T result = supplyAsync(() -> fn.apply(session)).get();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void connectCommitAndClose(Consumer<Session> fn) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try (session) {
            transaction = session.beginTransaction();
            executeAsync(() -> fn.accept(session)).get();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }
}
