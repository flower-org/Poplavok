package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableStopOrderQueryRequest.class)
@JsonDeserialize(as = ImmutableStopOrderQueryRequest.class)
public interface StopOrderQueryRequest extends PageRequest {

    @Nullable
    String symbol();

    @Nullable
    String side();

    @Nullable
    String type();

    @Nullable
    String tradeType();

    @Nullable
    Date startAt();

    @Nullable
    Date endAt();

    @Nullable
    List<String> orderIds();

/*    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public Date getStartAt() {
        return startAt;
    }

    public void setStartAt(Date startAt) {
        this.startAt = startAt;
    }

    public Date getEndAt() {
        return endAt;
    }

    public void setEndAt(Date endAt) {
        this.endAt = endAt;
    }

    public StopOrderQueryRequest addOrderId(String orderId) {
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
