package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableCurrencyDetailV2Response.class)
@JsonDeserialize(as = ImmutableCurrencyDetailV2Response.class)
public interface CurrencyDetailV2Response {
    /** Support margin or not */
    @Nullable
    Boolean isMarginEnabled();

    /** Support debit or not */
    @Nullable
    Boolean isDebitEnabled();

    /** chain of currency */
    @Nullable
    List<ApiCurrencyDetailChainPropertyResponse> chains();

    /** A unique currency code that will never change */
    @Nullable
    String currency();

    /** Currency name, will change after renaming */
    @Nullable
    String name();

    /** Full name of a currency, will change after renaming */
    @Nullable
    String fullName();

    /** Currency precision */
    @Nullable
    String precision();

    /** Number of block confirmations */
    @Nullable
    String confirms();

    /** Contract address */
    @Nullable
    String contractAddress();
}
