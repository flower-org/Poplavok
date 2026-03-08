/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * 订单创建对象
 *
 * @author 屈亮
 * @since 2018-09-17
 */
@Value.Immutable
@JsonSerialize(as = ImmutableOrderCreateApiRequest.class)
@JsonDeserialize(as = ImmutableOrderCreateApiRequest.class)
public interface OrderCreateApiRequest {
    /**
     * a valid trading symbol code. e.g. ETH-BTC
     */
    @Nullable
    String symbol();

    /**
     * [optional] limit or market (default is limit)
     */
    default String type() {
        return "limit";
    }

    /**
     * buy or sell
     */
    @Nullable
    String side();

    /**
     * price per base currency
     */
    @Nullable
    BigDecimal price();

    /**
     * amount of base currency to buy or sell
     */
    @Nullable
    BigDecimal size();

    /**
     * [optional] Desired amount of quote currency to use
     */
    @Nullable
    BigDecimal funds();

    /**
     * [optional] self trade protect , CN, CO, CB or DC
     */
    default String stp() {
        return "";
    }

    /**
     * [Optional] The type of trading : TRADE（Spot Trade）, MARGIN_TRADE (Margin Trade). Default is TRADE
     */
    default String tradeType() {
        return "TRADE";
    }

    /**
     * [optional] GTC, GTT, IOC, or FOK (default is GTC)
     */
    default String timeInForce() {
        return "GTC";
    }

    /**
     * [optional] * cancel after n seconds
     */
    @Nullable
    Long cancelAfter();

    /**
     * [optional] ** Post only flag
     */
    @Nullable
    Boolean postOnly();

    /**
     * [optional] Orders not displayed in order book
     */
    @Nullable
    Boolean hidden();

    /**
     * [optional] Only visible portion of the order is displayed in the order book
     */
    @Nullable
    Boolean iceberg();

    /**
     * [optional] The maximum visible size of an iceberg order
     */
    @Nullable
    BigDecimal visibleSize();

    /**
     * Unique order id selected by you to identify your order e.g. UUID
     */
    @Nullable
    String clientOid();

    /**
     * [optional] remark for the order, length cannot exceed 100 utf8 characters
     */
    @Nullable
    String remark();
}