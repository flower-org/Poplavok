/*
 * Copyright 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.request;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableMultiOrderCreateRequest.class)
@JsonDeserialize(as = ImmutableMultiOrderCreateRequest.class)
public interface MultiOrderCreateRequest {

    @Nullable
    String symbol();

    @Nullable
    List<OrderCreateApiRequest> orderList();

    @Nullable
    String channel();
}
