package com.poplavok.forms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SellDownGameStateTest {
    @TempDir
    Path folder;

    @Test
    void roundTripsThroughYaml() throws Exception {
        SellDownGameState state = new SellDownGameState("LTC-USDT", "44.79", "19488.01257933", "363.693",
                "5788.11331399", "88.27", "94.5", "95", "0.2", "0.1", "0.1", "0.1");
        File file = folder.resolve("game.sdg.yaml").toFile();
        state.write(file);
        assertEquals(state, SellDownGameState.read(file));
    }

    @Test
    void missingAndUnknownFieldsAreTolerated() throws Exception {
        File file = folder.resolve("old.sdg.yaml").toFile();
        Files.writeString(file.toPath(), "symbol: LTC-USDT\ndebt: \"100\"\nsomethingNew: 1\n");
        SellDownGameState state = SellDownGameState.read(file);
        assertEquals("LTC-USDT", state.symbol());
        assertEquals("100", state.debt());
        assertNull(state.startRatio());
    }
}
