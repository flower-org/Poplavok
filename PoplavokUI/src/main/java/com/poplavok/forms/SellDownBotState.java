package com.poplavok.forms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.poplavok.data.utils.SellDownPlanner;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Sell-down bot definition, created by the approximator: the plan frozen at creation, which the bot acts upon in a
 * single sell-down cycle. Decimals are plain strings; ratios, buffer, slippage and fees are in %.
 * {@code price}, {@code debt}, {@code base}, {@code quote}: position the plan was calculated for.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SellDownBotState(String symbol, String createdAt, String price, String debt, String base, String quote,
                               String startRatio, String endRatio, String liquidationRatio,
                               String fillBuffer, String slippage, String makerFee, String takerFee,
                               List<Level> levels) {
    public static final String EXTENSION = "*.sdb";
    static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public enum Status { PENDING, SOLD, BROKEN_EVEN }

    /**
     * One sell level: sell {@code baseToSell} at {@code sellPrice}, starting at {@code sellTriggerPrice}
     * ({@code immediate}: at market right away). Break-even prices are null for level 1.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Level(int number, String targetRatio, String sellPrice, String sellTriggerPrice, boolean immediate,
                        String baseToSell, String expectedQuote, @Nullable String breakEvenPrice,
                        @Nullable String breakEvenTriggerPrice, Status status) {
    }

    public static SellDownBotState of(String symbol, SellDownPlanner.Params params, SellDownPlanner.Plan plan,
                                      Instant createdAt) {
        return new SellDownBotState(symbol, createdAt.toString(), plain(params.price()), plain(params.debt()),
                plain(params.base()), plain(params.quote()), percent(params.startRatio()),
                percent(params.endRatio()), percent(params.liquidationRatio()), percent(params.fillBuffer()),
                percent(params.slippage()), percent(params.makerFee()), percent(params.takerFee()),
                plan.levels().stream().map(level -> new Level(level.number(), percent(level.targetRatio()),
                        plain(level.sellPrice()), plain(level.sellTriggerPrice()), level.immediate(),
                        plain(level.baseSold()), plain(level.quoteReceived()), plainOrNull(level.breakEvenPrice()),
                        plainOrNull(level.breakEvenTriggerPrice()), Status.PENDING)).toList());
    }

    static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    static @Nullable String plainOrNull(@Nullable BigDecimal value) {
        return value == null ? null : plain(value);
    }

    static String percent(BigDecimal fraction) {
        return plain(fraction.movePointRight(2));
    }

    public void write(File file) throws IOException {
        MAPPER.writeValue(file, this);
    }

    public static SellDownBotState read(File file) throws IOException {
        return MAPPER.readValue(file, SellDownBotState.class);
    }
}
