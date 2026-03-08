package com.KyKu4.MogeJlb.response;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableApiCurrencyDetailChainPropertyResponse.class)
@JsonDeserialize(as = ImmutableApiCurrencyDetailChainPropertyResponse.class)
public interface ApiCurrencyDetailChainPropertyResponse {

    /** chain name of currency */
    @Nullable
    String chainName();

    @Nullable
    String chain();

    /** Minimum withdrawal amount */
    @Nullable
    BigDecimal withdrawalMinSize();

    /** Minimum fees charged for withdrawal */
    @Nullable
    BigDecimal withdrawalMinFee();

    /** Support withdrawal or not */
    @Nullable
    Boolean isWithdrawEnabled();

    /** Support deposit or not */
    @Nullable
    Boolean isDepositEnabled();

    @Nullable
    Integer confirms();

    @Nullable
    String contractAddress();
}