/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.websocket.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by chenshiwei on 2019/1/23.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableOrderActivateEvent.class)
@JsonDeserialize(as = ImmutableOrderActivateEvent.class)
public interface OrderActivateEvent {

    @Nullable
    String sequence();

    @Nullable
    String symbol();

    @Nullable
    String side();

    @Nullable
    BigDecimal size();

    @Nullable
    String orderId();

    @Nullable
    BigDecimal price();

    @Nullable
    String time();

    @Nullable
    String type();

    @Nullable
    String orderType();

    @Nullable
    BigDecimal funds();

    @Nullable
    BigDecimal remainSize();

    @Nullable
    String reason();

    @Nullable
    String takerOrderId();

    @Nullable
    String makerOrderId();

    @Nullable
    String tradeId();

    @Nullable
    BigDecimal newSize();

    @Nullable
    BigDecimal oldSize();

    @Nullable
    String stopType();

    @Nullable
    String clientOid();

}
