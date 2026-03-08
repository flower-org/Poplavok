/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.HashSet;
import java.util.Set;

@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableOrderCancelResponse.class)
@JsonDeserialize(as = ImmutableOrderCancelResponse.class)
public interface OrderCancelResponse {

    default Set<String> cancelledOrderIds() {
        return new HashSet<>();
    }
}
