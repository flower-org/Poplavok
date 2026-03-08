/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.KyKu4.MogeJlb.model.OrderBook;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Created by chenshiwei on 2019/1/18.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableOrderBookResponse.class)
@JsonDeserialize(as = ImmutableOrderBookResponse.class)
public interface OrderBookResponse extends OrderBook {

    @Nullable
    String sequence();

    @Nullable
    Long time();

}
