package com.poplavok.data.utils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static com.poplavok.data.utils.BigDecimalUtil.SCALE;

/**
 * Margin liquidation helpers based on KuCoin's debt ratio:
 * <pre>debtRatio = totalLiabilities / totalAssets</pre>
 * with both sides valued in QUOTE at the given price.
 * <ul>
 *     <li>Debt in QUOTE (long): {@code ratio = D / (B * P + Q)}; liquidation as price falls.</li>
 *     <li>Debt in BASE (short): {@code ratio = D * P / (B * P + Q)}; liquidation as price rises.</li>
 * </ul>
 */
public class LiquidationCalculator {
    /** KuCoin margin-call warning debt ratio, from GET /api/v1/margin/config ("warningDebtRatio"). */
    public static final BigDecimal KUCOIN_WARNING_DEBT_RATIO = new BigDecimal("0.92");

    private static final MathContext MC = MathContext.DECIMAL128;

    /** Returns the debt ratio at the given price, or null if total assets are zero. */
    public static @Nullable BigDecimal debtRatio(BigDecimal debt, boolean debtInQuote,
                                                 BigDecimal holdingBase, BigDecimal holdingQuote, BigDecimal price) {
        BigDecimal assets = holdingBase.multiply(price).add(holdingQuote);
        if (assets.signum() <= 0) {
            return null;
        }
        BigDecimal liabilities = debtInQuote ? debt : debt.multiply(price);
        // Round up: never understate how close the position is to liquidation
        return liabilities.divide(assets, SCALE, RoundingMode.CEILING);
    }

    /**
     * Price at which the position reaches {@code debtRatio}.
     *
     * @return the price, or null if no positive price reaches that ratio
     * (e.g. holdings in the debt currency alone already cover the debt).
     */
    public static @Nullable BigDecimal priceAtDebtRatio(BigDecimal debt, boolean debtInQuote,
                                                        BigDecimal holdingBase, BigDecimal holdingQuote,
                                                        BigDecimal debtRatio) {
        if (debt.signum() <= 0 || debtRatio.signum() <= 0) {
            return null;
        }
        BigDecimal price;
        if (debtInQuote) {
            // D / (B*P + Q) = r  =>  P = (D/r - Q) / B
            if (holdingBase.signum() <= 0) {
                return null;
            }
            price = debt.divide(debtRatio, MC).subtract(holdingQuote).divide(holdingBase, MC);
        } else {
            // D*P / (B*P + Q) = r  =>  P = r*Q / (D - r*B)
            BigDecimal denominator = debt.subtract(debtRatio.multiply(holdingBase));
            if (denominator.signum() <= 0) {
                return null;
            }
            price = debtRatio.multiply(holdingQuote).divide(denominator, MC);
        }
        return price.signum() > 0 ? price.setScale(SCALE, RoundingMode.HALF_UP) : null;
    }

    public static @Nullable BigDecimal liquidationPrice(BigDecimal debt, boolean debtInQuote,
                                                        BigDecimal holdingBase, BigDecimal holdingQuote,
                                                        BigDecimal liquidationDebtRatio) {
        return priceAtDebtRatio(debt, debtInQuote, holdingBase, holdingQuote, liquidationDebtRatio);
    }

    /** Converts a percent value (e.g. 95) to a ratio (0.95). */
    public static BigDecimal percentToRatio(BigDecimal percent) {
        return percent.movePointLeft(2);
    }

    /** Signed % price change from {@code currentPrice} to {@code targetPrice}, e.g. 100 -> 90 is -10. */
    public static BigDecimal priceMoveToPercent(BigDecimal currentPrice, BigDecimal targetPrice) {
        return targetPrice.subtract(currentPrice).divide(currentPrice, MC).movePointRight(2);
    }
}
