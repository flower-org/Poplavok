/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Date;

@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize(as = ImmutableStopOrderResponse.class)
@JsonDeserialize(as = ImmutableStopOrderResponse.class)
public interface StopOrderResponse {

    @Nullable
    String id();

    @Nullable
    String symbol();

    @Nullable
    String userId();

    @Nullable
    String type();

    @Nullable
    String side();

    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal funds();

    @Nullable
    String stp();

    @Nullable
    String timeInForce();

    @Nullable
    Long cancelAfter();

    @Nullable
    Boolean postOnly();

    @Nullable
    Boolean hidden();

    @Nullable
    Boolean iceberg();

    @Nullable
    BigDecimal visibleSize();

    @Nullable
    String channel();

    @Nullable
    String clientOid();

    @Nullable
    String feeCurrency();

    @Nullable
    String tradeSource();

    @Nullable
    String tradeType();

    @Nullable
    String stop();

    @Nullable
    BigDecimal stopPrice();

    @Nullable
    String remark();

    @Nullable
    String tags();

    @Nullable
    Date createdAt();

    @Nullable
    Date stopTriggerTime();

}
