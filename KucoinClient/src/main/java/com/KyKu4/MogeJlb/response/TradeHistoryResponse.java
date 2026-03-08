/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by chenshiwei on 2019/1/18.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableTradeHistoryResponse.class)
@JsonDeserialize(as = ImmutableTradeHistoryResponse.class)
public interface TradeHistoryResponse {

    @Nullable
    String sequence();

    @Nullable
    BigDecimal price();

    @Nullable
    BigDecimal size();

    @Nullable
    String side();

    @Nullable
    Long time();

}
