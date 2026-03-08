/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.KyKu4.MogeJlb.model.enums.WithdrawStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by zicong.lu on 2018/12/21.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableWithdrawResponse.class)
@JsonDeserialize(as = ImmutableWithdrawResponse.class)
public interface WithdrawResponse {

    @Nullable
    String id();

    @Nullable
    String currency();

    @Nullable
    WithdrawStatusEnum status();

    @Nullable
    String address();

    @Nullable
    String memo();

    @JsonProperty("isInner")
    @Nullable
    Boolean isInner();

    @Nullable
    BigDecimal amount();

    @Nullable
    BigDecimal fee();

    @Nullable
    String walletTxId();

    @Nullable
    Date createdAt();

    @Nullable
    Date updatedAt();

    @Nullable
    String remark();
}
