package com.poplavok.data.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellDownPlannerTest {
    static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    static void assertClose(String expected, BigDecimal actual) {
        assertTrue(bd(expected).subtract(actual).abs().compareTo(bd("0.0001")) < 0,
                "expected " + expected + " but was " + actual);
    }

    static SellDownPlanner.Params params(String buffer, String fee) {
        // Debt 800 USDT, 1 BASE at 1000 -> debt ratio 80%
        return new SellDownPlanner.Params(bd("1000"), bd("800"), bd("1"), bd("0"),
                bd("0.85"), bd("0.90"), 2, bd(buffer), bd("0"), bd(fee), bd(fee), bd("0.95"));
    }

    @Test
    void autoLevelsKeepTwoBuffersBetweenSellPrices() {
        // Debt 800 at price 1000, sell from 85% to 94.5%, buffer 1% -> sell prices must be >= 2% apart
        SellDownPlanner.Params p = new SellDownPlanner.Params(bd("1000"), bd("800"), bd("10"), bd("0"),
                bd("0.85"), bd("0.945"), 0, bd("0.01"), bd("0.001"), bd("0.001"), bd("0.001"), bd("0.95"));
        SellDownPlanner.Plan plan = SellDownPlanner.planAutoLevels(p);
        int n = plan.levels().size();
        assertTrue(n > 1 && n < SellDownPlanner.MAX_AUTO_LEVELS, "levels: " + n);
        assertTrue(SellDownPlanner.isSpaced(plan, bd("0.02")));
        assertTrue(SellDownPlanner.isAveragingClear(plan));
        // One more level would break one of the rules
        SellDownPlanner.Plan more = SellDownPlanner.plan(SellDownPlanner.withLevels(p, n + 1));
        assertFalse(SellDownPlanner.isSpaced(more, bd("0.02")) && SellDownPlanner.isAveragingClear(more));
    }

    @Test
    void breakEvenSpendsQuoteFromAllLevelsAbove() {
        SellDownPlanner.Params p = new SellDownPlanner.Params(bd("1000"), bd("800"), bd("10"), bd("50"),
                bd("0.85"), bd("0.945"), 4, bd("0.002"), bd("0.001"), bd("0.001"), bd("0.001"), bd("0.95"));
        SellDownPlanner.Plan plan = SellDownPlanner.plan(p);
        assertNull(SellDownPlanner.breakEven(p, plan, plan.levels().get(0)));

        SellDownPlanner.Level third = plan.levels().get(2);
        SellDownPlanner.BreakEven be = SellDownPlanner.breakEven(p, plan, third);
        assertNotNull(be);
        BigDecimal buyPrice = third.breakEvenPrice();
        assertNotNull(buyPrice);
        assertClose(buyPrice.toPlainString(), be.price());
        assertClose("800", be.debt());
        assertClose("50", be.quote());
        BigDecimal spent = plan.levels().get(0).quoteReceived().add(plan.levels().get(1).quoteReceived())
                .add(third.quoteReceived());
        assertClose(third.baseLeft().add(spent.multiply(bd("0.999")).divide(buyPrice,
                java.math.MathContext.DECIMAL128)).toPlainString(), be.base());
        // Level 1 sold higher than level 3's break-even price, so more BASE comes back than was sold
        assertTrue(be.base().compareTo(bd("10")) > 0);
    }

    @Test
    void breakEvenRecoversBothChunksTriggeredOneBufferBelow() {
        SellDownPlanner.Plan plan = SellDownPlanner.plan(params("0.002", "0.01"));
        SellDownPlanner.Level first = plan.levels().get(0);
        SellDownPlanner.Level second = plan.levels().get(1);
        assertNull(first.breakEvenPrice());
        assertNull(first.breakEvenTriggerPrice());
        BigDecimal breakEven = second.breakEvenPrice();
        BigDecimal buy = second.breakEvenTriggerPrice();
        assertNotNull(breakEven);
        assertNotNull(buy);
        // Spending both levels' QUOTE at break-even (1% buy fee) recovers exactly both chunks
        BigDecimal quote = first.quoteReceived().add(second.quoteReceived()).multiply(bd("0.99"));
        assertClose("1", quote.divide(breakEven, java.math.MathContext.DECIMAL128));
        assertClose(breakEven.multiply(bd("0.998")).toPlainString(), buy);
        assertTrue(buy.compareTo(second.sellTriggerPrice()) > 0 && breakEven.compareTo(first.sellPrice()) < 0);
    }

    @Test
    void twoLevelsNoBufferNoFee() {
        SellDownPlanner.Plan plan = SellDownPlanner.plan(params("0", "0"));
        assertEquals(2, plan.levels().size());

        SellDownPlanner.Level first = plan.levels().get(0);
        assertClose("941.17647059", first.ratioPrice()); // 800 / 0.85
        assertClose("0.5", first.baseSold());
        assertClose("0.85", checkedRatio(first));
        assertFalse(first.immediate());

        SellDownPlanner.Level second = plan.levels().get(1);
        // (800/0.9 - 470.588235) / 0.5
        assertClose("836.60130719", second.ratioPrice());
        assertClose("0", second.baseLeft());
        assertNull(second.liqPriceAfter());

        assertClose("0.9", checkNotNullRatio(plan.finalRatio()));
        assertClose("0", plan.finalBase());
    }

    @Test
    void sellsAtRatioPriceTriggeredOneBufferAbove() {
        SellDownPlanner.Level first = SellDownPlanner.plan(params("0.002", "0")).levels().get(0);
        assertFalse(first.immediate());
        assertClose(first.ratioPrice().toPlainString(), first.sellPrice());
        assertClose(first.sellPrice().multiply(bd("1.002")).toPlainString(), first.sellTriggerPrice());
        assertClose("0.85", checkedRatio(first));
    }

    @Test
    void oneChunkLeftStillHasLiquidationPrice() {
        SellDownPlanner.Plan plan = SellDownPlanner.plan(params("0.002", "0.001"));
        SellDownPlanner.Level last = plan.levels().get(plan.levels().size() - 1);
        assertNotNull(last.liqPriceIfUnfilled());
        assertTrue(last.liqPriceIfUnfilled().compareTo(last.sellPrice()) < 0);
    }

    @Test
    void alreadyAboveStartSellsImmediately() {
        SellDownPlanner.Params p = new SellDownPlanner.Params(bd("1000"), bd("870"), bd("1"), bd("0"),
                bd("0.85"), bd("0.90"), 2, bd("0.002"), bd("0.001"), bd("0"), bd("0.01"), bd("0.95"));
        SellDownPlanner.Level first = SellDownPlanner.plan(p).levels().get(0);
        assertTrue(first.immediate());
        // Market minus 0.1% slippage, taker fee 1%
        assertClose("999", first.sellPrice());
        assertClose("494.505", first.quoteReceived());
    }

    @Test
    void consecutiveImmediateLevelsAllSellAtMarketMinusSlippage() {
        // Debt 900 at price 1000 -> 90%, already past both levels
        SellDownPlanner.Params p = new SellDownPlanner.Params(bd("1000"), bd("900"), bd("1"), bd("0"),
                bd("0.85"), bd("0.88"), 3, bd("0.002"), bd("0.001"), bd("0"), bd("0"), bd("0.95"));
        SellDownPlanner.Plan plan = SellDownPlanner.plan(p);
        assertTrue(plan.levels().get(0).immediate());
        assertTrue(plan.levels().get(1).immediate());
        assertClose("999", plan.levels().get(0).sellPrice());
        assertClose("999", plan.levels().get(1).sellPrice());
    }

    @Test
    void stopsWhenQuoteCoversDebt() {
        SellDownPlanner.Params p = new SellDownPlanner.Params(bd("1000"), bd("100"), bd("1"), bd("200"),
                bd("0.85"), bd("0.90"), 3, bd("0"), bd("0"), bd("0"), bd("0"), bd("0.95"));
        SellDownPlanner.Plan plan = SellDownPlanner.plan(p);
        assertTrue(plan.levels().isEmpty());
        assertNotNull(plan.note());
    }

    @Test
    void rejectsEndRatioAtLiquidation() {
        SellDownPlanner.Params p = new SellDownPlanner.Params(bd("1000"), bd("800"), bd("1"), bd("0"),
                bd("0.85"), bd("0.95"), 2, bd("0"), bd("0"), bd("0"), bd("0"), bd("0.95"));
        assertThrows(IllegalArgumentException.class, () -> SellDownPlanner.plan(p));
    }

    static BigDecimal checkedRatio(SellDownPlanner.Level level) {
        return checkNotNullRatio(level.ratioAfter());
    }

    static BigDecimal checkNotNullRatio(@javax.annotation.Nullable BigDecimal ratio) {
        assertNotNull(ratio);
        return ratio;
    }
}
