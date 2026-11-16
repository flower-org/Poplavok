package com.poplavok.api.kucoin.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/** Trading rules of a symbol from GET /api/v2/symbols/{symbol}. */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableSymbolResponse.class)
@JsonDeserialize(as = ImmutableSymbolResponse.class)
public interface SymbolResponse {
    @Nullable
    String symbol();

    @Nullable
    String baseCurrency();

    @Nullable
    String quoteCurrency();

    /** Minimum order size in BASE. */
    @Nullable
    BigDecimal baseMinSize();

    /** Order sizes must be a multiple of this. */
    @Nullable
    BigDecimal baseIncrement();

    /** Limit prices must be a multiple of this. */
    @Nullable
    BigDecimal priceIncrement();

    /** Minimum order value in QUOTE. */
    @Nullable
    BigDecimal minFunds();

    @Nullable
    Boolean enableTrading();
}
