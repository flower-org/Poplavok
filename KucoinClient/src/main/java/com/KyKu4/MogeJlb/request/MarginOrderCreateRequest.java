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
 * @since 2018-09-17
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMarginOrderCreateRequest.class)
@JsonDeserialize(as = ImmutableMarginOrderCreateRequest.class)
public interface MarginOrderCreateRequest {
    /**
     * Unique order id selected by you to identify your order e.g. UUID
     */
    @Nullable
    String clientOid();

    /**
     * buy or sell
     */
    @Nullable
    String side();

    /**
     * a valid trading symbol code. e.g. ETH-BTC
     */
    @Nullable
    String symbol();

    /**
     * [optional] limit or market (default is limit)
     */
    @Value.Default
    default String type() {
        return "limit";
    }

    /**
     * [optional] remark for the order, length cannot exceed 100 utf8 characters
     */
    @Nullable
    String remark();

    /**
     * [optional] self trade protect, CN, CO, CB or DC
     * ‘DC’ Decrease and Cancel
     * ‘CO’ Cancel oldest
     * ‘CN’ Cancel newest
     * ‘CB’ Cancel both
     */
    //@Value.Default
    //default String stp() { return ""; }
    @Nullable
    String stp();

    /**
     * [Optional] The type of trading, including `cross` (cross mode) and `isolated` (isolated mode). It is set at cross by default.
     * The isolated mode will be released soon, so stay tuned!
     */
    @Value.Default
    default String marginModel() {
        return "cross";
    }

    /**
     * [Optional] Auto-borrow to place order. The system will first borrow you funds at the optimal interest rate
     * and then place an order for you.
     */
    @Nullable
    @Value.Default
    default Boolean autoBorrow() { return false; }


    /**
     * price per base currency
     */
    @Nullable
    BigDecimal price();

    /**
     * amount of BASE currency to buy or sell
     */
    @Nullable
    BigDecimal size();

    /**
     * [optional] GTC, GTT, IOC, or FOK (default is GTC)
     */
    @Value.Default
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
     * [optional] Desired amount of quote currency to use
     */
    @Nullable
    BigDecimal funds();

}