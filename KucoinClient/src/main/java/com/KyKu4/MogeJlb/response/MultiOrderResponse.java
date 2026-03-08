/*
 * Copyright 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.response;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableMultiOrderResponse.class)
@JsonDeserialize(as = ImmutableMultiOrderResponse.class)
public interface MultiOrderResponse {

    @Nullable
    String symbol();

    default String type() {
        return "limit";
    }

    @Nullable
    String side();

    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal funds();

    default String stp() {
        return "";
    }

    default String stop() {
        return "";
    }

    @Nullable
    BigDecimal stopPrice();

    default String timeInForce() {
        return "GTC";
    }

    @Nullable
    Long cancelAfter();

    default boolean postOnly() {
        return false;
    }

    default boolean hidden() {
        return false;
    }

    default boolean iceberge() {
        return false;
    }

    default boolean iceberg() {
        return false;
    }

    @Nullable
    BigDecimal visibleSize();

    @Nullable
    String channel();

    /*public void setIceberg(boolean iceberg) {
        if (!iceberge && iceberg) {
            this.iceberge = iceberg;
        }
    }*/

    @Nullable
    String id();

    @Nullable
    String status();

    @Nullable
    String failMsg();

    @Nullable
    String clientOid();
}
