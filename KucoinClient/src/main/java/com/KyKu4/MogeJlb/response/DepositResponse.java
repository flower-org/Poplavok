/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

import com.KyKu4.MogeJlb.model.enums.DepositStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Created by zicong.lu on 2018/12/21.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableDepositResponse.class)
@JsonDeserialize(as = ImmutableDepositResponse.class)
public interface DepositResponse {

    @Nullable
    String currency();

    @Nullable
    DepositStatusEnum status();

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
    Long createdAt();

    @Nullable
    Long updatedAt();

    @Nullable
    String remark();
}
