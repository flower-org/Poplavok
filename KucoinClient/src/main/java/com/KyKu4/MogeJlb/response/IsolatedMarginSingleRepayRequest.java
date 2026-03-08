package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableIsolatedMarginSingleRepayRequest.class)
@JsonDeserialize(as = ImmutableIsolatedMarginSingleRepayRequest.class)
public interface IsolatedMarginSingleRepayRequest {
    /** Trading pair, e.g.: BTC-USDT */
    @Nullable
    String symbol();
    /** Repayment coin type */
    @Nullable
    String currency();
    /** Repayment amount */
    @Nullable
    BigDecimal size();
    /** Trade order number; when this field is configured, the sequence strategy is invalidated */
    @Nullable
    String loanId();
}
