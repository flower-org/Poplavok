package com.KyKu4.MogeJlb.websocket.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableStopOrderEvent.class)
@JsonDeserialize(as = ImmutableStopOrderEvent.class)
public interface StopOrderEvent {

    @Nullable
    String stop();

    @Nullable
    BigDecimal stopPrice();

    @Nullable
    Boolean triggerSuccess();

    @Nullable
    String orderId();

    @Nullable
    String symbol();

    @Nullable
    String type();

    @Nullable
    String orderType();

    @Nullable
    String side();

    @Nullable
    String error();

    @Nullable
    String tradeType();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal orderPrice();

    @Nullable
    Long createdAt();

    @Nullable
    Long ts();
}
