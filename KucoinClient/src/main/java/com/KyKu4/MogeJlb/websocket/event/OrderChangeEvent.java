package com.KyKu4.MogeJlb.websocket.event;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableOrderChangeEvent.class)
@JsonDeserialize(as = ImmutableOrderChangeEvent.class)
public interface OrderChangeEvent {

    @Nullable
    String orderId();

    @Nullable
    String symbol();

    @Nullable
    String type();

    @Nullable
    String status();

    @Nullable
    BigDecimal matchPrice();

    @Nullable
    BigDecimal matchSize();

    @Nullable
    String orderType();

    @Nullable
    String side();

    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal filledSize();

    @Nullable
    BigDecimal oldSize();

    @Nullable
    String tradeId();

    @Nullable
    String clientOid();

    @Nullable
    Long orderTime();

    @Nullable
    String liquidity();

    @Nullable
    BigDecimal remainSize();

    @Nullable
    BigDecimal remainFunds();

    @Nullable
    Long ts();

    @Nullable
    BigDecimal funds();
}
