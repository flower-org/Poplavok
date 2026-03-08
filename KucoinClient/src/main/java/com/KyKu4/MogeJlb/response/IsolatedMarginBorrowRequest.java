package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Value.Immutable
@JsonSerialize(as = ImmutableIsolatedMarginBorrowRequest.class)
@JsonDeserialize(as = ImmutableIsolatedMarginBorrowRequest.class)
public interface IsolatedMarginBorrowRequest {
    /** Trading pair, e.g.: BTC-USDT */
    @Nullable
    String symbol();
    /** Borrowed coin type */
    @Nullable
    String currency();
    /** Borrowed amount */
    @Nullable
    BigDecimal size();
    /** Borrowing strategy:
     * FOK - All or nothing + IOC
     * IOC - immediate or cancel, execute immediately and cancel unfilled
     **/
    @Nullable
    String borrowStrategy();
    /** Max interest rate, defaults to all interest rates if left blank */
    @Nullable
    BigDecimal maxRate();
    /** The term in days. Defaults to all terms if left blank. 7,14,28 */
    @Nullable
    String period();
}
