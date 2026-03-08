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
@JsonSerialize(as = ImmutableUserFeeResponse.class)
@JsonDeserialize(as = ImmutableUserFeeResponse.class)
public interface UserFeeResponse {

    @Nullable
    String symbol();

    @Nullable
    BigDecimal takerFeeRate();

    @Nullable
    BigDecimal makerFeeRate();

}
