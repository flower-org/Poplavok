/*
 * Copyright 2019 Mek Global Limited
 */

package com.KyKu4.MogeJlb.response;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableMultiOrderCreateResponse.class)
@JsonDeserialize(as = ImmutableMultiOrderCreateResponse.class)
public interface MultiOrderCreateResponse {

    @Nullable
    List<MultiOrderResponse> data();
}
