/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by chenshiwei on 2019/1/19.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableOrderBook.class)
@JsonDeserialize(as = ImmutableOrderBook.class)
public interface OrderBook {

    /**
     * [price, size] for aggregated, [orderId, price, size] for atomic
     */
    @Nullable
    List<List<String>> asks();

    /**
     * [price, size] for aggregated, [orderId, price, size] for atomic
     */
    @Nullable
    List<List<String>> bids();

}
