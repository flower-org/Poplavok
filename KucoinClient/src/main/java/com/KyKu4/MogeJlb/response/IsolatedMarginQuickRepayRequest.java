package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableIsolatedMarginQuickRepayRequest.class)
@JsonDeserialize(as = ImmutableIsolatedMarginQuickRepayRequest.class)
public interface IsolatedMarginQuickRepayRequest {
    /** Trading pair, e.g.: BTC-USDT */
    String symbol();
    /** Repayment coin type */
    String currency();
    /** Repayment amount */
    BigDecimal size();
    /** Repayment sequence strategy,
     * RECENTLY_EXPIRE_FIRST: Maturity date priority (the loan with the closest maturity is repaid first),
     * HIGHEST_RATE_FIRST: Interest rate priority (the loan with the highest interest rate is repaid first)
     */
    String seqStrategy();
}
