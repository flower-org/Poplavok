/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableStopOrderCreateRequest.class)
@JsonDeserialize(as = ImmutableStopOrderCreateRequest.class)
public interface StopOrderCreateRequest {

    @Nullable
    String symbol();

    default String type() {
        return "limit";
    }

    @Nullable
    String side();

    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal funds();

    default String tradeType() {
        return "TRADE";
    }

    @Nullable
    String stp();

    default String stop() {
        return "loss";
    }

    @Nullable
    BigDecimal stopPrice();

    default String timeInForce() {
        return "GTC";
    }

    @Nullable
    Boolean postOnly();

    @Nullable
    Boolean hidden();

    @Nullable
    Boolean iceberg();

    @Nullable
    BigDecimal visibleSize();

    @Nullable
    Long cancelAfter();

    @Nullable
    String clientOid();

    @Nullable
    String remark();

    @Nullable
    String tags();

}
