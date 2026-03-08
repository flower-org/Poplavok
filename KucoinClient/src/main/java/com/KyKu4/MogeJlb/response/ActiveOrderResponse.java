/*
 * Copyright 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Date;

@Value.Immutable
@JsonSerialize(as = ImmutableActiveOrderResponse.class)
@JsonDeserialize(as = ImmutableActiveOrderResponse.class)
public interface ActiveOrderResponse {

    @Nullable
    String id();

    @Nullable
    String symbol();

    @Nullable
    String opType();

    @Nullable
    String type();

/*    public String getType() {
        return this.type == null ? null : this.type.toLowerCase();
    }*/

    @Nullable
    String side();

/*    public String getSide() {
        return this.side == null ? null : this.side.toLowerCase();
    }*/

    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal funds();

    @Nullable
    BigDecimal dealFunds();

    @Nullable
    BigDecimal dealSize();

    @Nullable
    BigDecimal fee();

    @Nullable
    String feeCurrency();

    @Nullable
    String stp();

    @Nullable
    String stop();

    /*public String getStop() {
        return this.stop == null ? null : this.stop.toLowerCase();
    }*/

    @Nullable
    Boolean stopTriggered();

    @Nullable
    BigDecimal stopPrice();

    @Nullable
    String timeInForce();

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
    String channel();

    @Nullable
    String clientOid();

    @Nullable
    String remark();

    @Nullable
    String tags();

    @Nullable
    Boolean isActive();

    @Nullable
    Boolean cancelExist();

    @Nullable
    Date createdAt();

    @Nullable
    String tradeType();

/*    public String getTradeType() {
        if (StringUtils.isBlank(tradeType)) {
            return "TRADE";
        }
        return tradeType;
    }*/
}
