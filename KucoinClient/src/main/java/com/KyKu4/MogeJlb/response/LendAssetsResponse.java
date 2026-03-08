/*
 * Copyright (c) 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableLendAssetsResponse.class)
@JsonDeserialize(as = ImmutableLendAssetsResponse.class)
public interface LendAssetsResponse {

    @Nullable
    String currency();

    @Nullable
    BigDecimal outstanding();

    @Nullable
    BigDecimal filledSize();

    @Nullable
    BigDecimal accruedInterest();

    @Nullable
    BigDecimal realizedProfit();

    @Nullable
    Boolean isAutoLend();

}
