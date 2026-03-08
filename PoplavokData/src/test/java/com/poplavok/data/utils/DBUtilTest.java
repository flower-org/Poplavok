package com.poplavok.data.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class DBUtilTest {

    @Test
    public void testRunAsync() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        CompletableFuture<Void> future = DBUtil.runAsync(() -> {
            executed.set(true);
        });

        future.get(5, TimeUnit.SECONDS);
        Assertions.assertTrue(executed.get());
    }

    @Test
    public void testSupplyAsync() throws Exception {
        CompletableFuture<String> future = DBUtil.supplyAsync(() -> "Hello");
        Assertions.assertEquals("Hello", future.get(5, TimeUnit.SECONDS));
    }

    @Test
    public void testSchedule() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        DBUtil.schedule(() -> executed.set(true), 100, TimeUnit.MILLISECONDS);

        Assertions.assertFalse(executed.get());

        Thread.sleep(200);

        Assertions.assertTrue(executed.get());
    }

    @Test
    public void testConnectGetResultAndClose() {
        String result = DBUtil.connectGetResultAndClose(session -> "Success");
        assertEquals("Success", result);
    }
}
