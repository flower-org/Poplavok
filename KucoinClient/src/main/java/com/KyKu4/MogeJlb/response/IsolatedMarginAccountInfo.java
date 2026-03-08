package com.KyKu4.MogeJlb.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableIsolatedMarginAccountInfo.class)
@JsonDeserialize(as = ImmutableIsolatedMarginAccountInfo.class)
public interface IsolatedMarginAccountInfo {
    /** The total balance of the isolated margin account (in the specified coin) */
    @Nullable
    String totalConversionBalance();
    /** Total liabilities of the isolated margin account (in the specified coin) */
    @Nullable
    String liabilityConversionBalance();
    /** Account list */
    @Nullable
    List<Asset> assets();

    @Value.Immutable
    @JsonSerialize(as = ImmutableAsset.class)
    @JsonDeserialize(as = ImmutableAsset.class)
    interface Asset {
        /** Trading pairs, with each trading pair indicating a position */
        @Nullable
        String symbol();
        /** The position status: Existing liabilities-DEBT, No liabilities-CLEAR, Bankrupcy (after position enters a negative balance)-BANKRUPTCY, Existing borrowings-IN_BORROW, Existing repayments-IN_REPAY, Under liquidation-IN_LIQUIDATION, Under auto-renewal assets-IN_AUTO_RENEW. */
        @Nullable
        String status();
        /** Debt ratio */
        @Nullable
        String debtRatio();

        /** base coin type asset info */
        @Nullable
        AssetDetail baseAsset();
        /** quote coin type asset info */
        @Nullable
        AssetDetail quoteAsset();

        @Value.Immutable
        @JsonSerialize(as = ImmutableAssetDetail.class)
        @JsonDeserialize(as = ImmutableAssetDetail.class)
        interface AssetDetail {
            /** Coin type Code */
            @Nullable
            String currency();
            /** Current coin type asset amount */
            @Nullable
            BigDecimal totalBalance();
            /** Current coin type frozen */
            @Nullable
            BigDecimal holdBalance();
            /** The available balance (available assets - frozen assets) */
            BigDecimal availableBalance();

            @Nullable
            BigDecimal liability();
            @Nullable
            BigDecimal interest();
            @Nullable
            BigDecimal borrowableAmount();
        }
    }
}
