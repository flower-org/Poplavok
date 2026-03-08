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
@JsonSerialize(as = ImmutableAccountDetailResponse.class)
@JsonDeserialize(as = ImmutableAccountDetailResponse.class)
public interface AccountDetailResponse {

    @Nullable
    String id();

    @Nullable
    String currency();

    @Nullable
    BigDecimal amount();

    @Nullable
    BigDecimal fee();

    @Nullable
    BigDecimal balance();

    @Nullable
    String bizType();

    @Nullable
    String direction();

    @Nullable
    String createdAt();

    @Nullable
    String context();

}
