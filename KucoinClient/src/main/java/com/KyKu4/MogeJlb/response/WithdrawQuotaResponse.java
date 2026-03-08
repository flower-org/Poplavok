/**
 * Copyright 2019 Mek Global Limited.
 */
package com.KyKu4.MogeJlb.response;

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
@JsonSerialize(as = ImmutableWithdrawQuotaResponse.class)
@JsonDeserialize(as = ImmutableWithdrawQuotaResponse.class)
public interface WithdrawQuotaResponse {

    @Nullable
    String currency();

    @Nullable
    BigDecimal availableAmount();

    @Nullable
    BigDecimal remainAmount();

    @Nullable
    BigDecimal withdrawMinSize();

    @Nullable
    BigDecimal limitBTCAmount();

    @Nullable
    BigDecimal limitAmount();

    @Nullable
    BigDecimal innerWithdrawMinFee();

    @Nullable
    BigDecimal usedBTCAmount();

    @JsonProperty("isWithdrawEnabled")
    @Nullable
    Boolean isWithdrawEnabled();

    @Nullable
    BigDecimal withdrawMinFee();

    @Nullable
    Integer precision();

    @Nullable
    String chain();

}
