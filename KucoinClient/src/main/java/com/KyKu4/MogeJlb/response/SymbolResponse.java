/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by devin@kucoin.com on 2018-12-27.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableSymbolResponse.class)
@JsonDeserialize(as = ImmutableSymbolResponse.class)
public interface SymbolResponse {

    @Nullable
    String symbol();

    @Nullable
    String name();

    @Nullable
    String market();

    @Nullable
    String baseCurrency();

    @Nullable
    String quoteCurrency();

    @Nullable
    BigDecimal baseMinSize();

    @Nullable
    BigDecimal quoteMinSize();

    @Nullable
    BigDecimal baseMaxSize();

    @Nullable
    BigDecimal quoteMaxSize();

    @Nullable
    BigDecimal baseIncrement();

    @Nullable
    BigDecimal quoteIncrement();

    @Nullable
    BigDecimal priceIncrement();

    @Nullable
    String feeCurrency();

    @Nullable
    Boolean enableTrading();

    @Nullable
    Boolean isMarginEnabled();
}
