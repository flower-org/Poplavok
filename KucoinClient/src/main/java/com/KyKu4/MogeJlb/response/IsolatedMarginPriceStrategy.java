package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableIsolatedMarginPriceStrategy.class)
@JsonDeserialize(as = ImmutableIsolatedMarginPriceStrategy.class)
public interface IsolatedMarginPriceStrategy {
    @Nullable
    String baseBorrowCoefficient();

    @Nullable
    String quoteBorrowCoefficient();

    /** The trading pair code */
    @Nullable
    String symbol();

    /** Base currency type */
    @Nullable
    String baseCurrency();

    /** Quote coin */
    @Nullable
    String quoteCurrency();

    /** Trading pair name */
    @Nullable
    String symbolName();

    /** Maximum leverage */
    @Nullable
    String maxLeverage();

    /** Liquidation debt ratio */
    @Nullable
    String flDebtRatio();

    /** Trade switch */
    @Nullable
    String tradeEnable();

    /** During automatic renewal of the max debt ratio, the loan will only be renewed if it is lower than the debt ratio, with partial liquidation triggered for repayment if the debt ratio is in excess */
    @Nullable
    String autoRenewMaxDebtRatio();

    /** base coin type borrow switch */
    @Nullable
    String baseBorrowEnable();

    /** quote coin type borrow switch */
    @Nullable
    String quoteBorrowEnable();

    /** base coin type transfer switch */
    @Nullable
    String baseTransferInEnable();

    /** quote coin type transfer switch */
    @Nullable
    String quoteTransferInEnable();
}
