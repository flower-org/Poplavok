package com.poplavok.poll;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PushToPollAdapterTest {
    final PushToPollAdapter<String> adapter = new PushToPollAdapter<>(3);

    static List<String> values(CompletableFuture<List<PushToPollAdapter.Update<String>>> future) {
        return future.getNow(List.of()).stream().map(PushToPollAdapter.Update::value).toList();
    }

    @Test
    void returnsBufferedUpdatesNewerThanLastUpdate() {
        adapter.accept("a");
        adapter.accept("b");
        List<PushToPollAdapter.Update<String>> all = adapter.poll(0).join();
        assertEquals(List.of("a", "b"), all.stream().map(PushToPollAdapter.Update::value).toList());
        assertEquals(List.of("b"), values(adapter.poll(all.get(0).updateNo())));
    }

    @Test
    void pendingPollCompletesOnNextValue() {
        CompletableFuture<List<PushToPollAdapter.Update<String>>> pending = adapter.poll(0);
        assertFalse(pending.isDone());
        adapter.accept("a");
        assertEquals(List.of("a"), values(pending));
    }

    @Test
    void upToDateAndAheadWaitersBehaveCorrectly() {
        adapter.accept("a");
        long no = adapter.poll(0).join().get(0).updateNo();
        CompletableFuture<List<PushToPollAdapter.Update<String>>> upToDate = adapter.poll(no);
        CompletableFuture<List<PushToPollAdapter.Update<String>>> ahead = adapter.poll(no + 100);
        assertFalse(upToDate.isDone());
        adapter.accept("b");
        assertEquals(List.of("b"), values(upToDate));
        assertFalse(ahead.isDone());
    }

    @Test
    void keepsOnlyLastCapacityUpdates() {
        for (String v : List.of("a", "b", "c", "d", "e")) {
            adapter.accept(v);
        }
        List<PushToPollAdapter.Update<String>> all = adapter.poll(0).join();
        assertEquals(List.of("c", "d", "e"), all.stream().map(PushToPollAdapter.Update::value).toList());
        assertEquals(3, all.get(0).updateNo());
    }

    @Test
    void pollsFromAnyPointOfWrappedRing() {
        for (String v : List.of("a", "b", "c", "d", "e", "f", "g")) {
            adapter.accept(v);
        }
        // Buffered: e=5, f=6, g=7.
        assertEquals(List.of("e", "f", "g"), values(adapter.poll(Long.MIN_VALUE)));
        assertEquals(List.of("e", "f", "g"), values(adapter.poll(2)));
        assertEquals(List.of("f", "g"), values(adapter.poll(5)));
        assertEquals(List.of("g"), values(adapter.poll(6)));
        assertFalse(adapter.poll(7).isDone());
        assertFalse(adapter.poll(Long.MAX_VALUE).isDone());
    }

    @Test
    void ignoresNulls() {
        CompletableFuture<List<PushToPollAdapter.Update<String>>> pending = adapter.poll(0);
        adapter.accept(null);
        assertFalse(pending.isDone());
    }

    @Test
    void cancelledPollDoesNotBreakOthers() {
        CompletableFuture<List<PushToPollAdapter.Update<String>>> cancelled = adapter.poll(0);
        CompletableFuture<List<PushToPollAdapter.Update<String>>> live = adapter.poll(0);
        assertTrue(cancelled.cancel(false));
        assertEquals(1, adapter.waiterCount());
        adapter.accept("a");
        assertEquals(List.of("a"), values(live));
    }

    @Test
    void cancelledAndTimedOutPollsAreRemovedWithoutNewValues() throws Exception {
        for (int i = 0; i < 100; i++) {
            assertTrue(adapter.poll(0).cancel(false));
        }
        CompletableFuture<List<PushToPollAdapter.Update<String>>> timed = adapter.poll(0).orTimeout(1, TimeUnit.MILLISECONDS);
        assertThrows(Exception.class, timed::join);
        assertEquals(0, adapter.waiterCount());
    }

    @Test
    void closeCompletesPendingAndIgnoresLaterValues() {
        adapter.accept("a");
        CompletableFuture<List<PushToPollAdapter.Update<String>>> pending = adapter.poll(1);
        adapter.close();
        assertEquals(List.of(), pending.join());
        adapter.accept("b");
        assertEquals(List.of(), adapter.poll(0).join());
    }

    @Test
    void rejectsNonPositiveCapacityAndNegativeStartAfter() {
        assertThrows(IllegalArgumentException.class, () -> new PushToPollAdapter<String>(0));
        assertThrows(IllegalArgumentException.class, () -> new PushToPollAdapter<String>(3, -1));
    }

    @Test
    void numberingStartsAfterStartAfterAndWrapsCorrectly() {
        PushToPollAdapter<String> offset = new PushToPollAdapter<>(3, 100);
        assertEquals(100, offset.lastUpdateNo());
        CompletableFuture<List<PushToPollAdapter.Update<String>>> pending = offset.poll(0);
        offset.accept("a");
        assertEquals(List.of(101L), pending.join().stream().map(PushToPollAdapter.Update::updateNo).toList());

        for (String v : List.of("b", "c", "d", "e")) {
            offset.accept(v);
        }
        // Buffered: c=103, d=104, e=105.
        assertEquals(List.of("c", "d", "e"), values(offset.poll(0)));
        assertEquals(List.of("e"), values(offset.poll(104)));
        assertFalse(offset.poll(105).isDone());
        assertEquals(105, offset.lastUpdateNo());
    }

    @Test
    void lastUpdateNoIsFinalAfterClose() {
        adapter.accept("a");
        adapter.accept("b");
        adapter.close();
        adapter.accept("c");
        assertEquals(2, adapter.lastUpdateNo());
    }

    @Test
    void concurrentProducerAndPollerSeeEveryValueInOrder() throws Exception {
        int count = 10_000;
        PushToPollAdapter<Integer> big = new PushToPollAdapter<>(count);
        Thread producer = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                big.accept(i);
            }
        });
        producer.start();
        List<Integer> seen = new ArrayList<>();
        long last = 0;
        while (seen.size() < count) {
            for (PushToPollAdapter.Update<Integer> update : big.poll(last).get(5, TimeUnit.SECONDS)) {
                assertTrue(update.updateNo() > last);
                last = update.updateNo();
                seen.add(update.value());
            }
        }
        producer.join();
        for (int i = 0; i < count; i++) {
            assertEquals(i, seen.get(i));
        }
    }
}
