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
@JsonSerialize(as = ImmutableBorrowRepaidResponse.class)
@JsonDeserialize(as = ImmutableBorrowRepaidResponse.class)
public interface BorrowRepaidResponse {

    @Nullable
    String tradeId();

    @Nullable
    String currency();

    @Nullable
    BigDecimal principal();

    @Nullable
    BigDecimal interest();

    @Nullable
    BigDecimal repaidSize();

    @Nullable
    BigDecimal dailyIntRate();

    @Nullable
    Integer term();

    @Nullable
    Date repayTime();

}
