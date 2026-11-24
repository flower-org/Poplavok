package com.poplavok.poll;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// next() returns a future of a future, which Error Prone flags wherever the outer one is used inline.
@SuppressWarnings("FutureReturnValueIgnored")
class FlowEventBusTest {
    final FlowEventBus bus = new FlowEventBus();

    @Test
    void nextCompletesWhenFirstRegisteredFutureFires() {
        CompletableFuture<String> a = new CompletableFuture<>();
        CompletableFuture<String> b = new CompletableFuture<>();
        bus.register(a);
        bus.register(b);

        CompletableFuture<CompletableFuture<?>> next = bus.next();
        assertFalse(next.isDone());
        b.complete("b");
        assertSame(b, next.join());
    }

    @Test
    void deliversOneAtATimeInFiringOrder() {
        CompletableFuture<String> a = new CompletableFuture<>();
        CompletableFuture<String> b = new CompletableFuture<>();
        CompletableFuture<String> c = new CompletableFuture<>();
        bus.register(a);
        bus.register(b);
        bus.register(c);
        c.complete("c");
        a.complete("a");

        assertSame(c, bus.next().join());
        assertSame(a, bus.next().join());
        assertFalse(bus.next().isDone());
    }

    @Test
    void firedFutureIsDeliveredOnceAndMustBeReRegistered() {
        CompletableFuture<String> a = CompletableFuture.completedFuture("a");
        bus.register(a);
        assertSame(a, bus.next().join());
        CompletableFuture<CompletableFuture<?>> pending = bus.next();
        assertFalse(pending.isDone());

        // Re-registering watches it again; being done, it fires immediately.
        bus.register(a);
        assertSame(a, pending.join());
        assertFalse(bus.next().isDone());
    }

    @Test
    void registeringTwiceIsNoOp() {
        CompletableFuture<String> a = new CompletableFuture<>();
        bus.register(a);
        bus.register(a);
        a.complete("a");
        assertSame(a, bus.next().join());
        assertFalse(bus.next().isDone());
    }

    @Test
    void exceptionalAndCancelledCompletionsCountAsFired() {
        CompletableFuture<String> failed = new CompletableFuture<>();
        CompletableFuture<String> cancelled = new CompletableFuture<>();
        bus.register(failed);
        bus.register(cancelled);
        failed.completeExceptionally(new IllegalStateException("boom"));
        cancelled.cancel(false);
        assertSame(failed, bus.next().join());
        assertSame(cancelled, bus.next().join());
    }

    @Test
    void unregisteredFutureIsNotDelivered() {
        CompletableFuture<String> a = new CompletableFuture<>();
        CompletableFuture<String> b = new CompletableFuture<>();
        bus.register(a);
        bus.register(b);
        assertTrue(bus.unregister(a));
        a.complete("a");
        b.complete("b");
        assertSame(b, bus.next().join());

        // Fired but undelivered futures can be unregistered too.
        CompletableFuture<String> c = new CompletableFuture<>();
        bus.register(c);
        c.complete("c");
        assertTrue(bus.unregister(c));
        assertFalse(bus.next().isDone());
    }

    @Test
    void onlyOnePendingNextAtATime() {
        CompletableFuture<CompletableFuture<?>> pending = bus.next();
        assertThrows(IllegalStateException.class, bus::next);
        assertTrue(pending.cancel(false));
        assertFalse(bus.next().isDone());
    }

    @Test
    void cancellingPendingNextLosesNoEvents() {
        CompletableFuture<String> a = new CompletableFuture<>();
        bus.register(a);
        assertTrue(bus.next().cancel(false));
        a.complete("a");
        assertSame(a, bus.next().join());
    }

    @Test
    void consumerCallbackCanReEnterBus() {
        CompletableFuture<String> a = new CompletableFuture<>();
        CompletableFuture<String> b = new CompletableFuture<>();
        bus.register(a);
        List<CompletableFuture<?>> seen = new ArrayList<>();
        CompletableFuture<?> unused = bus.next().thenAccept(fired -> {
            seen.add(fired);
            bus.register(b);
            CompletableFuture<?> unused2 = bus.next().thenAccept(seen::add);
        });
        a.complete("a");
        b.complete("b");
        assertEquals(List.of(a, b), seen);
    }

    @Test
    void eventLoopOverPushToPollAdapters() throws Exception {
        FlowEventBus loop = new FlowEventBus();
        PushToPollAdapter<String> btc = new PushToPollAdapter<>(10);
        PushToPollAdapter<String> eth = new PushToPollAdapter<>(10);
        CompletableFuture<List<PushToPollAdapter.Update<String>>> btcPoll = btc.poll(0);
        CompletableFuture<List<PushToPollAdapter.Update<String>>> ethPoll = eth.poll(0);
        loop.register(btcPoll);
        loop.register(ethPoll);

        CompletableFuture<CompletableFuture<?>> next = loop.next();
        eth.accept("eth-1");
        assertSame(ethPoll, next.get(1, TimeUnit.SECONDS));
        assertEquals("eth-1", ethPoll.join().get(0).value());

        // Re-register the next ETH poll; BTC is still registered from before.
        loop.register(eth.poll(ethPoll.join().get(0).updateNo()));
        btc.accept("btc-1");
        assertSame(btcPoll, loop.next().get(1, TimeUnit.SECONDS));
    }

    @Test
    void concurrentFiringDeliversEveryFutureExactlyOnce() throws Exception {
        int count = 5000;
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompletableFuture<String> future = new CompletableFuture<>();
            futures.add(future);
            bus.register(future);
        }
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            int offset = t;
            Thread thread = new Thread(() -> {
                for (int i = offset; i < count; i += 4) {
                    futures.get(i).complete("v" + i);
                }
            });
            thread.start();
            threads.add(thread);
        }
        Set<CompletableFuture<?>> seen = new HashSet<>();
        int received = 0;
        while (received < count) {
            assertTrue(seen.add(bus.next().get(5, TimeUnit.SECONDS)));
            received++;
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(count, seen.size());
    }
}
