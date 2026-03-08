/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Date;

@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableTradeResponse.class)
@JsonDeserialize(as = ImmutableTradeResponse.class)
public interface TradeResponse {

    @Nullable
    String symbol();

    @Nullable
    String tradeId();

    @Nullable
    String orderId();

    @Nullable
    String counterOrderId();

    @Nullable
    String side();

    /*public String getSide() {
        return this.side == null ? null : this.side.toLowerCase();
    }*/

    @Nullable
    String liquidity();

    /*public String getLiquidity() {
        return this.liquidity == null ? null : this.liquidity.toLowerCase();
    }*/

    @Nullable
    Boolean forceTaker();

    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal funds();

    @Nullable
    BigDecimal fee();

    @Nullable
    BigDecimal feeRate();

    @Nullable
    String feeCurrency();

    @Nullable
    String domainId();

    @JsonProperty("type")
    @Nullable
    String orderType();

    @Nullable
    String stop();

    @JsonProperty("createdAt")
    @Nullable
    Date tradeCreatedAt();

    @Nullable
    String displayType();
}
