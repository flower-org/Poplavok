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
 * Created by chenshiwei on 2019/1/10.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableTickerResponse.class)
@JsonDeserialize(as = ImmutableTickerResponse.class)
public interface TickerResponse {

    @Nullable
    String sequence();

    BigDecimal bestAsk();

    BigDecimal bestBid();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal bestAskSize();

    @Nullable
    BigDecimal bestBidSize();

    Long time();

}
