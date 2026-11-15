package com.poplavok.api.kucoin.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/** Isolated margin trading pair configuration from GET /api/v1/isolated/symbols. */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableIsolatedSymbolResponse.class)
@JsonDeserialize(as = ImmutableIsolatedSymbolResponse.class)
public interface IsolatedSymbolResponse {
    String symbol();

    @Nullable
    String baseCurrency();

    @Nullable
    String quoteCurrency();

    @Nullable
    Integer maxLeverage();

    /** Forced liquidation debt ratio, e.g. 0.95. */
    @Nullable
    BigDecimal flDebtRatio();

    /** Max debt ratio at which loans are auto-renewed, e.g. 0.94. */
    @Nullable
    BigDecimal autoRenewMaxDebtRatio();

    @Nullable
    Boolean tradeEnable();

    @Nullable
    Boolean baseBorrowEnable();

    @Nullable
    Boolean quoteBorrowEnable();
}
