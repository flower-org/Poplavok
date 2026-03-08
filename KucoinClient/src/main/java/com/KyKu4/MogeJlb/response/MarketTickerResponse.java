package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by chenshiwei on 2019/2/22.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableMarketTickerResponse.class)
@JsonDeserialize(as = ImmutableMarketTickerResponse.class)
public interface MarketTickerResponse {

    @Nullable
    String symbol();

    @Nullable
    String symbolName();

    @Nullable
    BigDecimal buy();

    @Nullable
    BigDecimal sell();

    @Nullable
    BigDecimal changeRate();

    @Nullable
    BigDecimal changePrice();

    @Nullable
    BigDecimal high();

    @Nullable
    BigDecimal low();

    @Nullable
    BigDecimal vol();

    @Nullable
    BigDecimal last();

    @Nullable
    BigDecimal volValue();

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
    BigDecimal bestBidSize();

    @Nullable
    BigDecimal bestAskSize();

}
