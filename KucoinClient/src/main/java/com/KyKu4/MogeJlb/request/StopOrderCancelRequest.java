/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableStopOrderCancelRequest.class)
@JsonDeserialize(as = ImmutableStopOrderCancelRequest.class)
public interface StopOrderCancelRequest {

    @Nullable
    String symbol();

    @Nullable
    String tradeType();

    @Nullable
    List<String> orderIds();
/*
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public StopOrderCancelRequest addOrderId(String orderId) {
        if (orderIds == null) {
            orderIds = new ArrayList<>();
        }
        orderIds.add(orderId);
        return this;
    }

    public void setOrderIds(List<String> orderIds) {
        this.orderIds = orderIds;
    }

    public String getOrderIds() {
        if (orderIds == null) {
            return null;
        }
        return String.join(",", orderIds);
    }*/
}