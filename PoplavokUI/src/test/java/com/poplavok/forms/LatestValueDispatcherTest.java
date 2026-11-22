package com.poplavok.forms;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatestValueDispatcherTest {
    @Test
    void coalescesBurstIntoSingleDeliveryOfLatestValue() {
        List<Runnable> queued = new ArrayList<>();
        List<Integer> delivered = new ArrayList<>();
        LatestValueDispatcher<Integer> dispatcher = new LatestValueDispatcher<>(queued::add, delivered::add);

        dispatcher.submit(1);
        dispatcher.submit(2);
        dispatcher.submit(3);
        assertEquals(1, queued.size());

        queued.remove(0).run();
        assertEquals(List.of(3), delivered);

        dispatcher.submit(4);
        assertEquals(1, queued.size());
        queued.remove(0).run();
        assertEquals(List.of(3, 4), delivered);
    }
}
