package com.poplavok.forms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SellDownApproximatorStateTest {
    @TempDir
    Path folder;

    @Test
    void roundTripsThroughYaml() throws Exception {
        SellDownApproximatorState state = new SellDownApproximatorState("LTC-USDT", "44.79", "19488.01257933", "363.693",
                "5788.11331399", "88.27", "94.5", "95", "0.2", "0.1", "0.1", "0.1", true, true);
        File file = folder.resolve("approximation.sd").toFile();
        state.write(file);
        assertEquals(state, SellDownApproximatorState.read(file));
    }

    @Test
    void missingAndUnknownFieldsAreTolerated() throws Exception {
        File file = folder.resolve("old.sd").toFile();
        Files.writeString(file.toPath(), "symbol: LTC-USDT\ndebt: \"100\"\nsomethingNew: 1\n");
        SellDownApproximatorState state = SellDownApproximatorState.read(file);
        assertEquals("LTC-USDT", state.symbol());
        assertEquals("100", state.debt());
        assertNull(state.startRatio());
        assertNull(state.startRatioCurrent());
        assertNull(state.streamPrice());
    }
}
