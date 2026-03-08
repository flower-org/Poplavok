package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableMarginPriceStrategy.class)
@JsonDeserialize(as = ImmutableMarginPriceStrategy.class)
public interface MarginPriceStrategy {

    //Cross

    @Nullable
    String borrowCoefficient();

    /** Currency */
    @Nullable
    String currency();

    /** Maximum borrow amount */
    @Nullable
    BigDecimal borrowMaxAmount();

    /** Maximum buy amount */
    @Nullable
    BigDecimal buyMaxAmount();

    /** Maximum hold amount */
    @Nullable
    BigDecimal holdMaxAmount();

    /** Precision */
    @Nullable
    Integer precision();


    //Isolated

    @Nullable
    String baseBorrowCoefficient();

    @Nullable
    String quoteBorrowCoefficient();

    /** The valid trading symbol code. e.g: EOS-USDC. */
    @Nullable
    String symbol();

    /** Maximum borrowing amount of base currency */
    @Nullable
    String baseMaxBorrowAmount();

    /** Maximum borrowing amount of quote currency */
    @Nullable
    String quoteMaxBorrowAmount();

    /** Maximum buy amount of base currency */
    @Nullable
    String baseMaxBuyAmount();

    /** Maximum buy amount of quote currency */
    @Nullable
    String quoteMaxBuyAmount();

    /** Maximum holding amount of base currency */
    @Nullable
    String baseMaxHoldAmount();

    /** Maximum holding amount of quote currency */
    @Nullable
    String quoteMaxHoldAmount();

    /** Base currency precision */
    @Nullable
    String basePrecision();

    /** Quote currency precision */
    @Nullable
    String quotePrecision();
}
