package com.poplavok.forms;

import com.poplavok.bots.selldown.SellDownBotState;
import com.poplavok.data.utils.SellDownPlanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertEquals("LTC-USDT", state.definition().symbol());
        assertEquals("2026-11-15T21:00:00Z", state.definition().createdAt());
        assertEquals("44.79", state.definition().price());
        assertEquals("88.27", state.definition().startRatio());
        assertEquals("0.2", state.definition().fillBuffer());
        assertEquals(plan.levels().size(), state.definition().levels().size());
        assertEquals(plan.levels().size(), state.levels().size());
        assertFalse(state.levels().isEmpty());

        SellDownPlanner.Level first = plan.levels().get(0);
        SellDownBotState.LevelPlan frozen = state.plan(state.levels().get(0));
        assertEquals(1, frozen.number());
        assertEquals(0, new BigDecimal(frozen.sellPrice()).compareTo(first.sellPrice()));
        assertEquals(0, new BigDecimal(frozen.baseToSell()).compareTo(first.baseSold()));
        assertNull(frozen.breakEvenPrice());
        assertNotNull(state.plan(state.levels().get(1)).breakEvenPrice());
        state.levels().forEach(level -> assertEquals(SellDownBotState.Status.PENDING, level.status()));
        assertEquals(List.of(new SellDownBotState.Event("2026-11-15T21:00:00Z", SellDownBotState.EventType.CREATED,
                null, "44.79", null, null, null, plan.levels().size() + " level(s)")), state.history());
    }

    @Test
    void missingHistoryReadsAsEmpty() throws Exception {
        File file = folder.resolve("old.bot").toFile();
        Files.writeString(file.toPath(), "definition:\n  symbol: LTC-USDT\n");
        assertEquals(List.of(), SellDownBotState.read(file).history());
        assertNull(SellDownBotState.read(file).exchangeName());
    }

    @Test
    void botTabTexts() {
        SellDownPlanner.Params params = params();
        SellDownBotState state = SellDownBotState.of("LTC-USDT", params, SellDownPlanner.planAutoLevels(params),
                Instant.parse("2026-11-15T21:00:00Z"));
        assertEquals("LTC-USDT — single sell-down cycle, " + state.levels().size()
                + " level(s), created 2026-11-15T21:00:00Z, exchange: not bound yet. Not running.", SellDownBotTab.header(state));
        assertEquals("Sell-down Bot: LTC-USDT", SellDownBotTab.fileTitle(new File("/tmp/LTC-USDT.bot")));
        assertEquals("88.27%", SellDownBotTab.percent(state.definition().startRatio()));
        assertEquals(SellDownApproximatorTab.NONE, SellDownBotTab.amount(null));
    }

    @Test
    void roundTripsThroughYaml() throws Exception {
        SellDownPlanner.Params params = params();
        SellDownBotState state = SellDownBotState.of("LTC-USDT", params, SellDownPlanner.planAutoLevels(params),
                Instant.parse("2026-11-15T21:00:00Z"));
        state = state.withEvent(new SellDownBotState.Event("2026-11-15T21:05:00Z",
                SellDownBotState.EventType.ORDER_PLACED, 1, "44.5", "40.41", null, "abc123", null));
        com.poplavok.data.bot.BotExchange.Order order = new com.poplavok.data.bot.BotExchange.Order("abc123",
                com.poplavok.data.bot.BotExchange.Side.SELL, new BigDecimal("44.5"), new BigDecimal("40.41"),
                new BigDecimal("10"), new BigDecimal("445"), new BigDecimal("0.445"),
                com.poplavok.data.bot.BotExchange.OrderStatus.OPEN);
        state = state.withOrder(SellDownBotState.BotOrder.placed(1, order, Instant.parse("2026-11-15T21:05:00Z"))
                .cancelRequested(Instant.parse("2026-11-15T21:06:00Z")));
        File file = folder.resolve("LTC-USDT.bot").toFile();
        state = state.withExchangeName("SIMULATED");
        state.write(file);
        assertEquals(state, SellDownBotState.read(file));
        String yaml = Files.readString(file.toPath());
        assertTrue(yaml.contains("\ndefinition:\n  symbol: \"LTC-USDT\""), yaml);
        assertFalse(yaml.contains("\nsymbol:"), yaml);
    }
}
