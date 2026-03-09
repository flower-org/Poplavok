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
    String chainId();

    /** Minimum withdrawal amount */
    @Nullable
    BigDecimal minWithdrawSize();

    @Nullable
    BigDecimal minDepositSize();

    /** Minimum fees charged for withdrawal */
    @Nullable
    BigDecimal minWithdrawFee();

    @Nullable
    BigDecimal withdrawFeeRate();

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

    @Nullable
    Integer preConfirms();


    @Nullable
    Integer withdrawPrecision();

    @Nullable
    BigDecimal maxWithdrawSize();

    @Nullable
    BigDecimal maxDepositSize();

    @Nullable
    Boolean needTag();
}