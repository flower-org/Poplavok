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
@JsonSerialize(as = ImmutableOrderResponse.class)
@JsonDeserialize(as = ImmutableOrderResponse.class)
public interface OrderResponse {
    /** Order ID, the ID of an order: e.g. 5c35c02703aa673ceec2a168 */
    @Nullable
    String id();

    /** symbol: e.g. "BTC-USDT" */
    @Nullable
    String symbol();

    /** Operation type: DEAL */
    @Nullable
    String opType();

    /** order type: e.g. limit,market,stop_limit. */
    @Nullable
    String type();

    /** transaction direction: include buy and sell */
    @Nullable
    String side();

    /** order price */
    @Nullable
    BigDecimal price();

    /** order quantity */
    @Nullable
    BigDecimal size();

    /** order funds */
    @Nullable
    BigDecimal funds();

    /** executed size of funds */
    @Nullable
    BigDecimal dealFunds();

    /** deal quantity - executed quantity */
    @Nullable
    BigDecimal dealSize();

    /** fee */
    @Nullable
    BigDecimal fee();

    /** charge fee currency, e.g. "USDT" */
    @Nullable
    String feeCurrency();

    /** self trade prevention,include CN,CO,DC,CB */
    @Nullable
    String stp();

    /** stop type, include entry and loss */
    @Nullable
    String stop();

   /** stop order is triggered or not */
    @Nullable
    Boolean stopTriggered();

    /** stop price */
    @Nullable
    BigDecimal stopPrice();

    /** time InForce,include GTC,GTT,IOC,FOK */
    @Nullable
    String timeInForce();

    /** postOnly */
    @Nullable
    Boolean postOnly();

    /** hidden order */
    @Nullable
    Boolean hidden();

    /** iceberg order */
    @Nullable
    Boolean iceberg();

    /** display quantity for iceberg order */
    @Nullable
    BigDecimal visibleSize();

    /** cancel orders time，requires timeInForce to be GTT */
    @Nullable
    Long cancelAfter();

    /** order source */
    @Nullable
    String channel();

    /** user-entered order unique mark */
    @Nullable
    String clientOid();

    /** remark */
    @Nullable
    String remark();

    /** tag order source */
    @Nullable
    String tags();

    /** order status, true and false. If true, the order is active, if false, the order is fillled or cancelled
     * (status before unfilled or uncancelled)
     */
    @JsonProperty("isActive")
    @Nullable
    Boolean isActive();

    /** order cancellation transaction record */
    @Nullable
    Boolean cancelExist();

    /** created at timestamp */
    @Nullable
    Date createdAt();

    /** The type of trading */
    @Nullable
    String tradeType();
}
