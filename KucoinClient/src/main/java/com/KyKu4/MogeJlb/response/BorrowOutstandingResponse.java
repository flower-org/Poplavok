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
@JsonSerialize(as = ImmutableBorrowOutstandingResponse.class)
@JsonDeserialize(as = ImmutableBorrowOutstandingResponse.class)
public interface BorrowOutstandingResponse {

    @Nullable
    String tradeId();

    @Nullable
    String currency();

    @Nullable
    BigDecimal principal();

    @Nullable
    BigDecimal accruedInterest();

    @Nullable
    BigDecimal liability();

    @Nullable
    BigDecimal repaidSize();

    @Nullable
    BigDecimal dailyIntRate();

    @Nullable
    Integer term();

    @Nullable
    Date createdAt();

    @Nullable
    Date maturityTime();
}
