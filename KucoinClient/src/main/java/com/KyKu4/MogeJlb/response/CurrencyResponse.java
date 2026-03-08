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
 * Created by devin@kucoin.com on 2018-12-27.
 */
@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableCurrencyResponse.class)
@JsonDeserialize(as = ImmutableCurrencyResponse.class)
public interface CurrencyResponse {

    @Nullable
    String fullName();

    String currency();

    @Nullable
    String name();

    Integer precision();

    @Nullable
    BigDecimal withdrawalMinSize();

    @Nullable
    BigDecimal withdrawalMinFee();

    @JsonProperty("isWithdrawEnabled")
    @Nullable
    Boolean isWithdrawEnabled();

    @JsonProperty("isDepositEnabled")
    @Nullable
    Boolean isDepositEnabled();

    @Nullable
    Boolean isMarginEnabled();

    @Nullable
    Boolean isDebitEnabled();

}
