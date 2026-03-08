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
 * Created by chenshiwei on 2019/1/19.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMatchExcutionChangeEvent.class)
@JsonDeserialize(as = ImmutableMatchExcutionChangeEvent.class)
public interface MatchExcutionChangeEvent {

    @Nullable
    String sequence();

    @Nullable
    String symbol();

    @Nullable
    String side();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal price();

    @Nullable
    String takerOrderId();

    @Nullable
    Long time();

    @Nullable
    String type();

    @Nullable
    String makerOrderId();

    @Nullable
    String tradeId();

}
