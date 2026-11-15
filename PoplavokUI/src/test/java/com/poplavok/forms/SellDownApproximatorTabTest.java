package com.poplavok.forms;

import org.junit.jupiter.api.Test;

import com.poplavok.data.utils.SellDownPlanner;

import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellDownApproximatorTabTest {
    @Test
    void flagsRatioBelowDesignedStart() {
        assertEquals("Debt ratio: 50.00% (< 85%), liquidation at —",
                SellDownApproximatorTab.startState(new BigDecimal("0.5"), null, BigDecimal.ONE));
        assertEquals("Debt ratio: 84.99% (< 85%), liquidation at —",
                SellDownApproximatorTab.startState(new BigDecimal("0.8499"), null, BigDecimal.ONE));
    }

    @Test
    void noFlagAtOrAboveDesignedStart() {
        assertEquals("Debt ratio: 85.00%, liquidation at —",
                SellDownApproximatorTab.startState(new BigDecimal("0.84999"), null, BigDecimal.ONE));
        assertEquals("Debt ratio: 90.00%, liquidation at —",
                SellDownApproximatorTab.startState(new BigDecimal("0.9"), null, BigDecimal.ONE));
    }

    @Test
    void tabTitleFromFileName() {
        assertEquals("Sell-down: LTC-USDT", SellDownApproximatorTab.fileTitle(new File("/tmp/LTC-USDT.sd")));
        assertEquals("Sell-down: notes.txt", SellDownApproximatorTab.fileTitle(new File("notes.txt")));
    }

    @Test
    void cantContinueWhenBreakEvenLandsAtOrAboveEndRatio() {
        // Debt 945 against 10 BASE at 100: 94.5%
        SellDownPlanner.BreakEven be = new SellDownPlanner.BreakEven(9, new BigDecimal("100"), new BigDecimal("945"),
                BigDecimal.TEN, BigDecimal.ZERO);
        assertTrue(SellDownApproximatorTab.cantContinue(be, new BigDecimal("0.945")));
        assertFalse(SellDownApproximatorTab.cantContinue(be, new BigDecimal("0.946")));
    }

    @Test
    void breakEvenRatioAndLiquidationPrice() {
        // Debt 900 against 10 BASE at 100: 90%; 95% is hit at 900 / 0.95 / 10 = 94.7368...
        SellDownPlanner.BreakEven be = new SellDownPlanner.BreakEven(5, new BigDecimal("100"), new BigDecimal("900"),
                BigDecimal.TEN, BigDecimal.ZERO);
        assertEquals("90.00%", SellDownApproximatorTab.percent(SellDownApproximatorTab.breakEvenRatio(be)));
        BigDecimal liq = SellDownApproximatorTab.breakEvenLiqPrice(be, new BigDecimal("0.95"));
        assertEquals(new BigDecimal("94.7368"), liq == null ? null : liq.setScale(4, java.math.RoundingMode.DOWN));
    }
}
