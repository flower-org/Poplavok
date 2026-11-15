package com.poplavok.forms;

import com.poplavok.data.utils.SellDownPlanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SellDownBotStateTest {
    @TempDir
    Path folder;

    static SellDownPlanner.Params params() {
        return new SellDownPlanner.Params(new BigDecimal("44.79"), new BigDecimal("19488.01257933"),
                new BigDecimal("363.693"), new BigDecimal("5788.11331399"), new BigDecimal("0.8827"),
                new BigDecimal("0.945"), 0, new BigDecimal("0.002"), new BigDecimal("0.001"),
                new BigDecimal("0.001"), new BigDecimal("0.001"), new BigDecimal("0.95"));
    }

    @Test
    void freezesPlanAsPending() {
        SellDownPlanner.Params params = params();
        SellDownPlanner.Plan plan = SellDownPlanner.planAutoLevels(params);
        SellDownBotState state = SellDownBotState.of("LTC-USDT", params, plan,
                Instant.parse("2026-11-15T21:00:00Z"));

        assertEquals("LTC-USDT", state.symbol());
        assertEquals("2026-11-15T21:00:00Z", state.createdAt());
        assertEquals("44.79", state.price());
        assertEquals("88.27", state.startRatio());
        assertEquals("0.2", state.fillBuffer());
        assertEquals(plan.levels().size(), state.levels().size());
        assertFalse(state.levels().isEmpty());

        SellDownPlanner.Level first = plan.levels().get(0);
        SellDownBotState.Level frozen = state.levels().get(0);
        assertEquals(1, frozen.number());
        assertEquals(0, new BigDecimal(frozen.sellPrice()).compareTo(first.sellPrice()));
        assertEquals(0, new BigDecimal(frozen.baseToSell()).compareTo(first.baseSold()));
        assertNull(frozen.breakEvenPrice());
        assertNotNull(state.levels().get(1).breakEvenPrice());
        state.levels().forEach(level -> assertEquals(SellDownBotState.Status.PENDING, level.status()));
    }

    @Test
    void roundTripsThroughYaml() throws Exception {
        SellDownPlanner.Params params = params();
        SellDownBotState state = SellDownBotState.of("LTC-USDT", params, SellDownPlanner.planAutoLevels(params),
                Instant.parse("2026-11-15T21:00:00Z"));
        File file = folder.resolve("LTC-USDT.sdb").toFile();
        state.write(file);
        assertEquals(state, SellDownBotState.read(file));
    }
}
