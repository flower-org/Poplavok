package com.poplavok.data.utils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Sell-down game for a long margin position (debt in QUOTE): as the price falls, the remaining BASE is sold
 * in equal chunks at debt ratio levels spread evenly from {@code startRatio} to {@code endRatio}, so the position
 * ends up all QUOTE before reaching the liquidation ratio.
 * <p>
 * Each level's sell price is where the holdings left at that point reach the level's debt ratio; the chunk
 * is sold there and pays the maker fee. Selling starts at the sell trigger price, {@code fillBuffer} above it, so the
 * sell price isn't missed. If the trigger is already at/above the market (the level is already passed), the chunk is
 * sold right away at market minus {@code slippage} and pays the taker fee. The market price starts at the current
 * price and moves down to each resting order's sell price.
 * Every level N > 1 gets a break-even buy: the break-even price is the highest price at which the QUOTE received by
 * levels N-1 and N buys back (maker fee) all the BASE both sold. As the price rebounds, buying starts at the break-even
 * trigger price, one fill buffer below it. That range must stay above level N's sell range (up to its sell trigger).
 * <p>
 * Naming: each action has a price (where we hope to act) and a trigger price (where we start acting). Sell trigger =
 * sell price x (1 + fill buffer); break-even trigger = break-even price x (1 - fill buffer).
 * Selling doesn't change the debt ratio at the sell price (apart from the fee); it only reduces how
 * fast the ratio grows as the price keeps falling.
 */
public final class SellDownPlanner {
    private static final MathContext MC = MathContext.DECIMAL128;
    /** Upper bound for {@link #planAutoLevels}; also the result when the fill buffer is 0. */
    public static final int MAX_AUTO_LEVELS = 50;

    private SellDownPlanner() {
    }

    /** All ratios/fractions are plain fractions, e.g. 0.85 for 85%, 0.002 for 0.2%. */
    public record Params(BigDecimal price, BigDecimal debt, BigDecimal base, BigDecimal quote,
                         BigDecimal startRatio, BigDecimal endRatio, int levels,
                         BigDecimal fillBuffer, BigDecimal slippage, BigDecimal makerFee, BigDecimal takerFee,
                         BigDecimal liquidationRatio) {
    }

    /**
     * One sell level. {@code ratioPrice}: price where the remaining holdings reach the target ratio.
     * {@code sellPrice}: price the chunk is sold at, the ratio price (or market minus slippage if immediate).
     * {@code sellTriggerPrice}: ratio price plus the fill buffer, where selling starts. {@code immediate}: the level was already passed at the market price, so it sells right
     * away at market minus slippage. {@code liqPriceIfUnfilled}: liquidation price if this chunk does NOT fill (holdings before this
     * sale). {@code liqPriceAfter}: liquidation price after this sale, null once all BASE is sold.
     * {@code breakEvenPrice}: max price to buy back levels N-1 and N in full with their QUOTE, null for level 1.
     * {@code breakEvenTriggerPrice}: break-even price minus the fill buffer, where buying starts; null for level 1.
     */
    public record Level(int number, BigDecimal targetRatio, BigDecimal ratioPrice, BigDecimal sellPrice,
                        BigDecimal sellTriggerPrice, boolean immediate, BigDecimal baseSold, BigDecimal quoteReceived,
                        BigDecimal baseLeft, BigDecimal quoteAfter, @Nullable BigDecimal ratioAfter,
                        @Nullable BigDecimal liqPriceIfUnfilled, @Nullable BigDecimal liqPriceAfter,
                        @Nullable BigDecimal breakEvenPrice, @Nullable BigDecimal breakEvenTriggerPrice) {
    }

    /**
     * Plan result. {@code finalRatio}: debt ratio once everything that could be sold is sold (price independent if
     * all QUOTE), else null. {@code note}: why the plan stopped early, or null.
     */
    public record Plan(List<Level> levels, BigDecimal finalBase, BigDecimal finalQuote,
                       @Nullable BigDecimal finalRatio, @Nullable String note) {
    }

    /**
     * Plans with as many levels as possible (up to {@link #MAX_AUTO_LEVELS}) such that every chunk's sell price is
     * at least 2 x fill buffer (relative) above the next chunk's sell price, and every break-even trigger price is above
     * its level's sell price (outside that level's fill buffer range). {@code p.levels()} is ignored.
     */
    public static Plan planAutoLevels(Params p) {
        for (int n = MAX_AUTO_LEVELS; n > 1; n--) {
            Plan plan = plan(withLevels(p, n));
            if (isSpaced(plan, p.fillBuffer().multiply(BigDecimal.valueOf(2))) && isAveragingClear(plan)) {
                return plan;
            }
        }
        return plan(withLevels(p, 1));
    }

    /** True if each level's sell price is at least {@code minGap} (fraction of its price) above the next one's. */
    static boolean isSpaced(Plan plan, BigDecimal minGap) {
        List<Level> levels = plan.levels();
        for (int i = 0; i + 1 < levels.size(); i++) {
            BigDecimal maxNext = levels.get(i).sellPrice().multiply(BigDecimal.ONE.subtract(minGap), MC);
            if (levels.get(i + 1).sellPrice().compareTo(maxNext) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * True if each break-even range (from its trigger up) is above its level's sell range (up to the sell trigger; just
     * the sell price if sold immediately), so the buy and the sell can't both fill.
     */
    static boolean isAveragingClear(Plan plan) {
        for (Level level : plan.levels()) {
            BigDecimal buy = level.breakEvenTriggerPrice();
            BigDecimal rangeTop = level.immediate() ? level.sellPrice() : level.sellTriggerPrice();
            if (buy != null && buy.compareTo(rangeTop) <= 0) {
                return false;
            }
        }
        return true;
    }

    static Params withLevels(Params p, int levels) {
        return new Params(p.price(), p.debt(), p.base(), p.quote(), p.startRatio(), p.endRatio(), levels,
                p.fillBuffer(), p.slippage(), p.makerFee(), p.takerFee(), p.liquidationRatio());
    }

    /** Position after breaking even: same debt, BASE re-bought, QUOTE spent on it. */
    public record BreakEven(int fromLevel, BigDecimal price, BigDecimal debt, BigDecimal base, BigDecimal quote) {
    }

    /**
     * Break even from {@code level} (N > 1): at its break-even price, all QUOTE received by levels 1..N buys BASE
     * (maker fee). Returns null for level 1, or if the level isn't part of {@code plan}.
     */
    public static @Nullable BreakEven breakEven(Params p, Plan plan, Level level) {
        BigDecimal buyPrice = level.breakEvenPrice();
        int index = level.number() - 1;
        if (buyPrice == null || index < 0 || index >= plan.levels().size()
                || !plan.levels().get(index).equals(level)) {
            return null;
        }
        BigDecimal spent = BigDecimal.ZERO;
        for (int i = 0; i <= index; i++) {
            spent = spent.add(plan.levels().get(i).quoteReceived());
        }
        BigDecimal bought = LongShortCalculator.calculateBaseAmountToGetLong(spent, buyPrice, p.makerFee()).amount;
        return new BreakEven(level.number(), buyPrice, p.debt(), level.baseLeft().add(bought),
                level.quoteAfter().subtract(spent));
    }

    public static Plan plan(Params p) {
        validate(p);
        BigDecimal chunk = p.base().divide(BigDecimal.valueOf(p.levels()), MC);
        BigDecimal base = p.base();
        BigDecimal quote = p.quote();
        BigDecimal marketPrice = p.price();
        List<Level> levels = new ArrayList<>();
        String note = null;

        for (int i = 0; i < p.levels(); i++) {
            BigDecimal targetRatio = p.levels() == 1 ? p.startRatio()
                    : p.startRatio().add(p.endRatio().subtract(p.startRatio())
                            .multiply(BigDecimal.valueOf(i)).divide(BigDecimal.valueOf(p.levels() - 1L), MC));
            BigDecimal ratioPrice = LiquidationCalculator.priceAtDebtRatio(p.debt(), true, base, quote, targetRatio);
            if (ratioPrice == null) {
                note = "QUOTE alone keeps the debt ratio below " + percent(targetRatio)
                        + " at any price; remaining levels aren't needed.";
                break;
            }
            BigDecimal sellTrigger = ratioPrice.multiply(BigDecimal.ONE.add(p.fillBuffer()), MC);
            boolean immediate = sellTrigger.compareTo(marketPrice) >= 0;
            BigDecimal sellPrice = immediate ? marketPrice.multiply(BigDecimal.ONE.subtract(p.slippage()), MC)
                    : ratioPrice;
            BigDecimal fee = immediate ? p.takerFee() : p.makerFee();
            BigDecimal liqIfUnfilled = LiquidationCalculator.priceAtDebtRatio(p.debt(), true, base, quote,
                    p.liquidationRatio());

            // Last level sells whatever is left so division residue doesn't stay behind
            BigDecimal sold = i == p.levels() - 1 ? base : chunk.min(base);
            BigDecimal received = LongShortCalculator.calculateQuoteAmountToGetShort(sold, sellPrice, fee).amount;
            base = base.subtract(sold);
            quote = quote.add(received);

            Level previous = levels.isEmpty() ? null : levels.get(levels.size() - 1);
            BigDecimal breakEven = previous == null ? null
                    : PriceCalculator.calculateBuyPriceExact(previous.quoteReceived().add(received),
                            previous.baseSold().add(sold), p.makerFee()).price;
            levels.add(new Level(i + 1, targetRatio, ratioPrice, sellPrice, sellTrigger, immediate, sold, received, base, quote,
                    LiquidationCalculator.debtRatio(p.debt(), true, base, quote, sellPrice),
                    liqIfUnfilled,
                    base.signum() > 0
                            ? LiquidationCalculator.priceAtDebtRatio(p.debt(), true, base, quote, p.liquidationRatio())
                            : null,
                    breakEven,
                    breakEven == null ? null : breakEven.multiply(BigDecimal.ONE.subtract(p.fillBuffer()), MC)));
            if (!immediate) {
                marketPrice = sellPrice;
            }
        }

        BigDecimal finalRatio = base.signum() == 0 && quote.signum() > 0 ? p.debt().divide(quote, MC) : null;
        return new Plan(levels, base, quote, finalRatio, note);
    }

    static void validate(Params p) {
        if (p.price().signum() <= 0 || p.debt().signum() <= 0 || p.base().signum() <= 0) {
            throw new IllegalArgumentException("Price, debt and BASE must be positive");
        }
        if (p.quote().signum() < 0) {
            throw new IllegalArgumentException("QUOTE can't be negative");
        }
        if (p.levels() < 1) {
            throw new IllegalArgumentException("Need at least 1 level");
        }
        if (p.startRatio().signum() <= 0 || p.startRatio().compareTo(p.endRatio()) > 0
                || p.endRatio().compareTo(p.liquidationRatio()) >= 0) {
            throw new IllegalArgumentException("Need 0 < start ratio <= end ratio < liquidation ratio");
        }
        if (p.fillBuffer().signum() < 0 || p.slippage().signum() < 0 || p.slippage().compareTo(BigDecimal.ONE) >= 0
                || p.makerFee().signum() < 0 || p.makerFee().compareTo(BigDecimal.ONE) >= 0
                || p.takerFee().signum() < 0 || p.takerFee().compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Fill buffer, slippage and fees must be >= 0 (slippage/fees < 100%)");
        }
    }

    static String percent(BigDecimal ratio) {
        return ratio.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }
}
