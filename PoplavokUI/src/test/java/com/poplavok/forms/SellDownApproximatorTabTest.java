package com.poplavok.forms;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
