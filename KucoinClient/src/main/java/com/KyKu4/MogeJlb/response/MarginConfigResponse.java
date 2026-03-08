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
@JsonSerialize(as = ImmutableMarginConfigResponse.class)
@JsonDeserialize(as = ImmutableMarginConfigResponse.class)
public interface MarginConfigResponse {

    @Nullable
    List<String> currencyList();

    @Nullable
    Integer maxLeverage();

    @Nullable
    BigDecimal warningDebtRatio();

    @Nullable
    BigDecimal liqDebtRatio();
}
