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
@JsonSerialize(as = ImmutableAccountChangeEvent.class)
@JsonDeserialize(as = ImmutableAccountChangeEvent.class)
public interface AccountChangeEvent {

    @Nullable
    BigDecimal total();

    @Nullable
    BigDecimal available();

    @Nullable
    BigDecimal availableChange();

    @Nullable
    String currency();

    @Nullable
    BigDecimal hold();

    @Nullable
    BigDecimal holdChange();

    @Nullable
    String relationEvent();

    @Nullable
    String relationContext();

    @Nullable
    String relationEventId();

    @Nullable
    String time();

    @Nullable
    String accountId();

}
