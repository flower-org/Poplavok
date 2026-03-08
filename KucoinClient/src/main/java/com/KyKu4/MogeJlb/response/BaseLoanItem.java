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

/**
 * Created by shenteng on 2019-10-15.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableBaseLoanItem.class)
@JsonDeserialize(as = ImmutableBaseLoanItem.class)
public interface BaseLoanItem {

    @Nullable
    String orderId();

    @Nullable
    String currency();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal filledSize();

    @Nullable
    BigDecimal dailyIntRate();

    @Nullable
    Integer term();

    @Nullable
    Date createdAt();
}
