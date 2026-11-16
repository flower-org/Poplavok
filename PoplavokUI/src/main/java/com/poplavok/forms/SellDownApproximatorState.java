package com.poplavok.forms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * Sell-down approximator inputs as entered in the tab (ratios, buffer, slippage and fees in %), saved to/loaded from YAML.
 * {@code startRatio} null: keep the tab's start ratio (85% by default). {@code startRatioCurrent}: start ratio follows
 * the current debt ratio; null keeps the tab's setting. {@code streamPrice}: stream the pair's price after loading;
 * null/false keeps the saved price fixed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SellDownApproximatorState(@Nullable String symbol, @Nullable String price, @Nullable String debt,
                                @Nullable String base, @Nullable String quote, @Nullable String startRatio,
                                @Nullable String endRatio, @Nullable String liquidationRatio,
                                @Nullable String fillBuffer, @Nullable String slippage,
                                @Nullable String makerFee, @Nullable String takerFee,
                                @Nullable Boolean startRatioCurrent, @Nullable Boolean streamPrice) {
    public static final String EXTENSION = "*.def";
    static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public void write(File file) throws IOException {
        MAPPER.writeValue(file, this);
    }

    public static SellDownApproximatorState read(File file) throws IOException {
        return MAPPER.readValue(file, SellDownApproximatorState.class);
    }
}
