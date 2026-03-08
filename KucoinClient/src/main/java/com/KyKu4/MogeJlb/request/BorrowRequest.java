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
@JsonSerialize(as = ImmutableBorrowRequest.class)
@JsonDeserialize(as = ImmutableBorrowRequest.class)
public interface BorrowRequest {

    @Nullable
    String currency();

    @Nullable
    BigDecimal size();

    @Nullable
    BigDecimal maxRate();

    @Nullable
    String term();

    @Nullable
    String type();
}
