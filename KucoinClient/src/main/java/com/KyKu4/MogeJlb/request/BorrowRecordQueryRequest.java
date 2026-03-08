/*
 * Copyright (c) 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableBorrowRecordQueryRequest.class)
@JsonDeserialize(as = ImmutableBorrowRecordQueryRequest.class)
public interface BorrowRecordQueryRequest {

    @Nullable
    String currency();

    default Integer pageSize() {
        return 10;
    }

    default Integer currentPage() {
        return 1;
    }
}
