package com.poplavok.forms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * Sell-down game inputs as entered in the tab (ratios, buffer, slippage and fees in %), saved to/loaded from YAML.
 * {@code startRatio} null: let the tab derive it from the current debt ratio.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SellDownGameState(@Nullable String symbol, @Nullable String price, @Nullable String debt,
                                @Nullable String base, @Nullable String quote, @Nullable String startRatio,
                                @Nullable String endRatio, @Nullable String liquidationRatio,
                                @Nullable String fillBuffer, @Nullable String slippage,
                                @Nullable String makerFee, @Nullable String takerFee) {
    public static final String EXTENSION = "*.sdg.yaml";
    static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public void write(File file) throws IOException {
        MAPPER.writeValue(file, this);
    }

    public static SellDownGameState read(File file) throws IOException {
        return MAPPER.readValue(file, SellDownGameState.class);
    }
}
