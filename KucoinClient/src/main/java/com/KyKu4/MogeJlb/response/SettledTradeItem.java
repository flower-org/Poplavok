/*
 * Copyright (c) 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.response;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Date;

@Value.Immutable
@JsonSerialize(as = ImmutableSettledTradeItem.class)
@JsonDeserialize(as = ImmutableSettledTradeItem.class)
public interface SettledTradeItem {

    @Nullable
    String tradeId();

    @Nullable
    String currency();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal interest();

    @Nullable
    BigDecimal repaid();

    @Nullable
    BigDecimal dailyIntRate();

    @Nullable
    Integer term();

    @Nullable
    Date settledAt();

    @Nullable
    String note();
}
