/*
 * Copyright (c) 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableToggleAutoLendRequest.class)
@JsonDeserialize(as = ImmutableToggleAutoLendRequest.class)
public interface ToggleAutoLendRequest {

    @Nullable
    String currency();

    @Nullable
    Boolean isEnable();

    @Nullable
    BigDecimal retainSize();

    @Nullable
    BigDecimal dailyIntRate();

    @Nullable
    Integer term();
}
