/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by tao.mao on 2018/11/15.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableAccountHoldsResponse.class)
@JsonDeserialize(as = ImmutableAccountHoldsResponse.class)
public interface AccountHoldsResponse {

    @Nullable
    String currency();

    @Nullable
    BigDecimal holdAmount();

    @Nullable
    String bizType();

    @Nullable
    String orderId();

    @Nullable
    String createdAt();

    @Nullable
    String updatedAt();
}
