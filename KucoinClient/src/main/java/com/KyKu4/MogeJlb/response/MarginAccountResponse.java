/*
 * Copyright (c) 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableMarginAccountResponse.class)
@JsonDeserialize(as = ImmutableMarginAccountResponse.class)
public interface MarginAccountResponse {

    @Nullable
    BigDecimal debtRatio();

    @Nullable
    List<MarginAccount> accounts();
}
