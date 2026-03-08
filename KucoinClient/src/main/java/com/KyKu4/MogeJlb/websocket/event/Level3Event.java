package com.KyKu4.MogeJlb.websocket.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableLevel3Event.class)
@JsonDeserialize(as = ImmutableLevel3Event.class)
public interface Level3Event {

    @Nullable
    Long sequence();

    @Nullable
    Long ts();

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
    String makerOrderId();

    @Nullable
    String takerOrderId();

    @Nullable
    String clientOid();

    @Nullable
    BigDecimal price();

    @Nullable
    Long orderTime();

    @Nullable
    String reason();
}
