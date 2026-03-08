/*
 * Copyright (c) 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableBorrowResponse.class)
@JsonDeserialize(as = ImmutableBorrowResponse.class)
public interface BorrowResponse {

    @Nullable
    String orderId();

    @Nullable
    String currency();
}
