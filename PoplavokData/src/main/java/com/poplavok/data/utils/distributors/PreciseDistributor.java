package com.poplavok.data.utils.distributors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PreciseDistributor implements Distributor {

    public PreciseDistributor() {
    }

    private static class ShareItem {
        final int originalIndex;
        final BigDecimal initialAmount;
        BigDecimal calculatedAmount;
        final BigDecimal remainder;

        ShareItem(int index, BigDecimal initialAmount, BigDecimal calculatedAmount, BigDecimal remainder) {
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
        if (amounts.isEmpty()) {
            return List.of();
        }
        if (distributeAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("distributeAmount cannot be negative");
        }

        BigDecimal totalSum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal normalizedTarget = distributeAmount.setScale(scale, RoundingMode.DOWN);

        if (totalSum.compareTo(BigDecimal.ZERO) == 0) {
            if (normalizedTarget.compareTo(BigDecimal.ZERO) == 0) {
                List<BigDecimal> zeros = new ArrayList<>(amounts.size());
                for (int i = 0; i < amounts.size(); i++) {
                    zeros.add(BigDecimal.ZERO);
                }
                return zeros;
            }

            if (!allowOverdraft) {
                throw new RuntimeException("Distributed amount cannot be greater than the sum of all amounts, since overdraft is not enabled");
            }

            // Fair fallback when all balances are zero and overdraft is allowed.
            return distributeEvenly(amounts.size(), normalizedTarget, scale);
        }

        if (!allowOverdraft && normalizedTarget.compareTo(totalSum) > 0) {
            throw new RuntimeException("Distributed amount cannot be greater than the sum of all amounts, since overdraft is not enabled");
        }

        if (normalizedTarget.compareTo(BigDecimal.ZERO) == 0) {
            List<BigDecimal> zeros = new ArrayList<>(amounts.size());
            BigDecimal zeroAtScale = BigDecimal.ZERO.setScale(scale, RoundingMode.DOWN);
            for (int i = 0; i < amounts.size(); i++) {
                zeros.add(zeroAtScale);
            }
            return zeros;
        }

        List<ShareItem> items = new ArrayList<>();
        BigDecimal distributedSum = BigDecimal.ZERO;
        int exactScale = scale + 16;

        for (int i = 0; i < amounts.size(); i++) {
            BigDecimal amount = amounts.get(i);

            BigDecimal exactAmount = amount
                    .multiply(normalizedTarget)
                    .divide(totalSum, exactScale, RoundingMode.DOWN);
            BigDecimal calculatedAmount = exactAmount.setScale(scale, RoundingMode.DOWN);
            BigDecimal remainder = exactAmount.subtract(calculatedAmount);

            items.add(new ShareItem(i, amount, calculatedAmount, remainder));
            distributedSum = distributedSum.add(calculatedAmount);
        }

        BigDecimal remainingToDistribute = normalizedTarget.subtract(distributedSum);
        BigDecimal step = new BigDecimal("1").scaleByPowerOfTen(-scale);

        items.sort(Comparator.<ShareItem, BigDecimal>comparing(item -> item.remainder)
                .reversed()
                .thenComparingInt(item -> item.originalIndex));

        int itemIndex = 0;
        while (remainingToDistribute.compareTo(step) >= 0) {
            ShareItem currentItem = items.get(itemIndex);

            boolean canIncrease = allowOverdraft
                    || currentItem.calculatedAmount.add(step).compareTo(currentItem.initialAmount) <= 0;

            if (canIncrease) {
                currentItem.calculatedAmount = currentItem.calculatedAmount.add(step);
                remainingToDistribute = remainingToDistribute.subtract(step);
            }

            itemIndex = (itemIndex + 1) % items.size();

            // If overdraft is forbidden and no item can receive the remaining unit, stop.
            if (!allowOverdraft && itemIndex == 0) {
                boolean hasCandidate = false;
                for (ShareItem item : items) {
                    if (item.calculatedAmount.add(step).compareTo(item.initialAmount) <= 0) {
                        hasCandidate = true;
                        break;
                    }
                }
                if (!hasCandidate) {
                    break;
                }
            }
        }

        items.sort(Comparator.comparingInt(item -> item.originalIndex));

        List<BigDecimal> finalResult = new ArrayList<>();
        for (ShareItem item : items) {
            finalResult.add(item.calculatedAmount);
        }

        return finalResult;
    }

    private static List<BigDecimal> distributeEvenly(int count, BigDecimal target, int scale) {
        List<BigDecimal> result = new ArrayList<>(count);
        BigDecimal zeroAtScale = BigDecimal.ZERO.setScale(scale, RoundingMode.DOWN);
        for (int i = 0; i < count; i++) {
            result.add(zeroAtScale);
        }

        if (count == 0 || target.compareTo(BigDecimal.ZERO) == 0) {
            return result;
        }

        BigDecimal step = new BigDecimal("1").scaleByPowerOfTen(-scale);
        BigDecimal base = target.divide(BigDecimal.valueOf(count), scale, RoundingMode.DOWN);
        BigDecimal distributed = base.multiply(BigDecimal.valueOf(count));
        BigDecimal remainder = target.subtract(distributed);

        for (int i = 0; i < count; i++) {
            result.set(i, base);
        }

        int idx = 0;
        while (remainder.compareTo(step) >= 0) {
            result.set(idx, result.get(idx).add(step));
            remainder = remainder.subtract(step);
            idx = (idx + 1) % count;
        }

        return result;
    }
}
