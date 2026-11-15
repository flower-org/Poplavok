package com.poplavok.data.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LiquidationCalculatorTest {
    static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    static void assertPrice(String expected, BigDecimal actual) {
        assertEquals(0, bd(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void longPriceAtRatio() {
        // Debt 950 USDT, holding 1 BTC + 50 USDT: 950 / (P + 50) = 0.95 => P = 950
        BigDecimal price = LiquidationCalculator.priceAtDebtRatio(bd("950"), true, bd("1"), bd("50"), bd("0.95"));
        assertPrice("950", price);
        assertPrice("0.95", LiquidationCalculator.debtRatio(bd("950"), true, bd("1"), bd("50"), price));
    }

    @Test
    void shortPriceAtRatio() {
        // Debt 1 BTC, holding 0 BTC + 1000 USDT: P / 1000 = 0.95 => P = 950
        BigDecimal price = LiquidationCalculator.priceAtDebtRatio(bd("1"), false, bd("0"), bd("1000"), bd("0.95"));
        assertPrice("950", price);

        // Debt 2 BTC, holding 1 BTC + 1000 USDT: 2P / (P + 1000) = 0.85 => P = 850 / 1.15
        BigDecimal price2 = LiquidationCalculator.priceAtDebtRatio(bd("2"), false, bd("1"), bd("1000"), bd("0.85"));
        assertPrice("739.13043478", price2);
        assertPrice("0.85", LiquidationCalculator.debtRatio(bd("2"), false, bd("1"), bd("1000"), price2));
    }

    @Test
    void priceMoveToPercent() {
        assertPrice("-10", LiquidationCalculator.priceMoveToPercent(bd("100"), bd("90")));
        assertPrice("25", LiquidationCalculator.priceMoveToPercent(bd("80"), bd("100")));
    }

    @Test
    void unreachableReturnsNull() {
        // Quote holdings alone cover quote debt
        assertNull(LiquidationCalculator.priceAtDebtRatio(bd("100"), true, bd("1"), bd("1000"), bd("0.95")));
        // No base held with quote debt
        assertNull(LiquidationCalculator.priceAtDebtRatio(bd("100"), true, bd("0"), bd("50"), bd("0.95")));
        // Base holdings alone cover base debt
        assertNull(LiquidationCalculator.priceAtDebtRatio(bd("1"), false, bd("2"), bd("1000"), bd("0.95")));
        // No debt
        assertNull(LiquidationCalculator.priceAtDebtRatio(bd("0"), true, bd("1"), bd("50"), bd("0.95")));
    }
}
