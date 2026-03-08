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
@JsonSerialize(as = ImmutableLevel3ChangeEvent.class)
@JsonDeserialize(as = ImmutableLevel3ChangeEvent.class)
public interface Level3ChangeEvent {

    @Nullable
    String sequence();

    @Nullable
    String symbol();

    @Nullable
    String side();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal remainSize();

    @Nullable
    String orderId();

    @Nullable
    String clientOid();

    @Nullable
    BigDecimal price();

    @Nullable
    Long time();

    @Nullable
    String type();

    @Nullable
    String orderType();

    @Nullable
    String reason();
}
