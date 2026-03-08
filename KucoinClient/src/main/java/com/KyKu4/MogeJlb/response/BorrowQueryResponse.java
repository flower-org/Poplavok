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
@JsonSerialize(as = ImmutableBorrowQueryResponse.class)
@JsonDeserialize(as = ImmutableBorrowQueryResponse.class)
public interface BorrowQueryResponse {

    @Nullable
    String orderId();

    @Nullable
    String currency();

    @Nullable
    BigDecimal size();

    @Nullable
    String status();

    @Nullable
    BigDecimal filled();

    @Nullable
    List<MatchItem> matchList();
}
