/**
 * Copyright 2019 Mek Global Limited.
 */
package com.poplavok.api.kucoin.websocket.event;

import com.poplavok.api.kucoin.model.response.MarketTickerResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by chenshiwei on 2019/1/23.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableTickerChangeEvent.class)
@JsonDeserialize(as = ImmutableTickerChangeEvent.class)
public interface TickerChangeEvent extends MarketTickerResponse {
    
    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal bestBid();

    @Nullable
    BigDecimal bestAsk();

    @Nullable
    BigDecimal size();

    @Nullable
    String sequence();
}
