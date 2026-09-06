package com.poplavok.data.utils.distributors;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PreciseDistributor implements Distributor {

    public PreciseDistributor() {
    }

    private static class ShareItem {
        final int originalIndex;
        final BigInteger initialAmount;
        BigInteger calculatedAmount;
        final BigInteger remainder;

        ShareItem(int index, BigInteger initialAmount, BigInteger calculatedAmount, BigInteger remainder) {
            this.originalIndex = index;
            this.initialAmount = initialAmount;
            this.calculatedAmount = calculatedAmount;
            this.remainder = remainder;
        }
    }

    @Override
    public List<BigDecimal> distribute(List<BigDecimal> amounts, BigDecimal distributeAmount, int scale, boolean allowOverdraft) {
        if (amounts == null || distributeAmount == null) {
            throw new IllegalArgumentException("amounts and distributeAmount cannot be null");
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale must be >= 0");
        }
        if (distributeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("distributeAmount cannot be negative");
        }

        BigDecimal normalizedTarget = distributeAmount.setScale(scale, RoundingMode.DOWN);
        if (amounts.isEmpty()) {
            if (normalizedTarget.signum() == 0) {
                return List.of();
            }
            throw new IllegalArgumentException("amounts cannot be empty for a positive distribution amount");
        }

        BigInteger totalSum = BigInteger.ZERO;
        List<BigInteger> normalizedAmounts = new ArrayList<>(amounts.size());
        for (BigDecimal amount : amounts) {
            BigInteger normalizedAmount = amount.setScale(scale, RoundingMode.DOWN)
                    .movePointRight(scale)
                    .toBigIntegerExact();
            normalizedAmounts.add(normalizedAmount);
            totalSum = totalSum.add(normalizedAmount);
        }
        BigInteger normalizedTargetUnits = normalizedTarget.movePointRight(scale).toBigIntegerExact();

        if (totalSum.signum() == 0) {
            if (normalizedTargetUnits.signum() == 0) {
                return zeroResults(amounts.size(), 0);
            }

            if (!allowOverdraft) {
                throw new IllegalStateException("Distributed amount cannot be represented without overdraft at the requested scale");
            }

            // Fair fallback when all balances are zero and overdraft is allowed.
            return distributeEvenly(amounts.size(), normalizedTargetUnits, scale);
        }

        if (!allowOverdraft && normalizedTargetUnits.compareTo(totalSum) > 0) {
            throw new RuntimeException("Distributed amount cannot be greater than the sum of all amounts, since overdraft is not enabled");
        }

        if (normalizedTargetUnits.signum() == 0) {
            return zeroResults(amounts.size(), scale);
        }

        List<ShareItem> items = new ArrayList<>();
        BigInteger distributedSum = BigInteger.ZERO;

        for (int i = 0; i < amounts.size(); i++) {
            BigInteger amount = normalizedAmounts.get(i);
            BigInteger exactNumerator = amount.multiply(normalizedTargetUnits);
            BigInteger calculatedAmount = exactNumerator.divide(totalSum);
            BigInteger remainder = exactNumerator.remainder(totalSum);

            items.add(new ShareItem(i, amount, calculatedAmount, remainder));
            distributedSum = distributedSum.add(calculatedAmount);
        }

        BigInteger remainingToDistribute = normalizedTargetUnits.subtract(distributedSum);

        items.sort(Comparator.<ShareItem, BigInteger>comparing(item -> item.remainder)
                .reversed()
                .thenComparingInt(item -> item.originalIndex));

        int remainingUnits = remainingToDistribute.intValueExact();
        int itemIndex = 0;
        for (int unitIndex = 0; unitIndex < remainingUnits; unitIndex++) {
            ShareItem currentItem = null;
            for (int checked = 0; checked < items.size(); checked++) {
                ShareItem candidate = items.get(itemIndex);
                itemIndex = (itemIndex + 1) % items.size();
                if (allowOverdraft || candidate.calculatedAmount.add(BigInteger.ONE).compareTo(candidate.initialAmount) <= 0) {
                    currentItem = candidate;
                    break;
                }
            }
            if (currentItem == null) {
                throw new IllegalStateException("Unable to distribute the remaining amount without overdraft");
            }
            currentItem.calculatedAmount = currentItem.calculatedAmount.add(BigInteger.ONE);
        }

        items.sort(Comparator.comparingInt(item -> item.originalIndex));

        List<BigDecimal> finalResult = new ArrayList<>();
        for (ShareItem item : items) {
            finalResult.add(new BigDecimal(item.calculatedAmount, scale));
        }

        return finalResult;
    }

    private static List<BigDecimal> distributeEvenly(int count, BigInteger target, int scale) {
        List<BigDecimal> result = zeroResults(count, scale);
        if (count == 0 || target.signum() == 0) {
            return result;
        }

        BigInteger[] quotientAndRemainder = target.divideAndRemainder(BigInteger.valueOf(count));
        BigInteger base = quotientAndRemainder[0];
        int extraUnits = quotientAndRemainder[1].intValueExact();
        for (int i = 0; i < count; i++) {
            BigInteger value = base.add(i < extraUnits ? BigInteger.ONE : BigInteger.ZERO);
            result.set(i, new BigDecimal(value, scale));
        }
        return result;
    }

    private static List<BigDecimal> zeroResults(int count, int scale) {
        List<BigDecimal> result = new ArrayList<>(count);
        BigDecimal zero = BigDecimal.ZERO.setScale(scale, RoundingMode.DOWN);
        for (int i = 0; i < count; i++) {
            result.add(zero);
        }
        return result;
    }
}
