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
 * @author yi.yang
 * @since 2018/12/26.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableSymbolTickResponse.class)
@JsonDeserialize(as = ImmutableSymbolTickResponse.class)
public interface SymbolTickResponse {

    @Nullable
    String symbol();

    @Nullable
    BigDecimal changeRate();

    @Nullable
    BigDecimal changePrice();

    @Nullable
    BigDecimal open();

    @Nullable
    BigDecimal close();

    @Nullable
    BigDecimal high();

    @Nullable
    BigDecimal low();

    @Nullable
    BigDecimal vol();

    @Nullable
    BigDecimal volValue();

    @Nullable
    BigDecimal last();

    @Nullable
    BigDecimal buy();

    @Nullable
    BigDecimal sell();

    @Nullable
    BigDecimal averagePrice();

    @Nullable
    BigDecimal takerFeeRate();

    @Nullable
    BigDecimal makerFeeRate();

    @Nullable
    BigDecimal takerCoefficient();

    @Nullable
    BigDecimal makerCoefficient();

    @Nullable
    Long time();

}
