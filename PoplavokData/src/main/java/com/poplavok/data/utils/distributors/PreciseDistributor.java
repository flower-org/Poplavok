package com.poplavok.data.utils.distributors;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PreciseDistributor implements Distributor {

    public PreciseDistributor() {
    }

    private static class ShareItem {
        int originalIndex;
        BigDecimal initialAmount;
        BigDecimal weight;
        BigDecimal exactAmount;
        BigDecimal calculatedAmount;
        BigDecimal remainder;

        ShareItem(int index, BigDecimal initialAmount, BigDecimal weight, BigDecimal exactAmount, BigDecimal calculatedAmount) {
            this.originalIndex = index;
            this.initialAmount = initialAmount;
            this.weight = weight;
            this.exactAmount = exactAmount;
            this.calculatedAmount = calculatedAmount;
            this.remainder = exactAmount.subtract(calculatedAmount);
        }
    }

    @Override
    public List<BigDecimal> distribute(List<BigDecimal> amounts, BigDecimal distributeAmount, int scale, boolean allowOverdraft) {
        BigDecimal totalSum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSum.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Sum of amounts cannot be zero.");
        }

        List<ShareItem> items = new ArrayList<>();
        BigDecimal distributedSum = BigDecimal.ZERO;
        MathContext mc = new MathContext(10, RoundingMode.HALF_UP);

        for (int i = 0; i < amounts.size(); i++) {
            BigDecimal amount = amounts.get(i);
            BigDecimal weight = amount.divide(totalSum, mc);
            BigDecimal exactAmount = distributeAmount.multiply(weight);
            BigDecimal calculatedAmount = exactAmount.setScale(scale, RoundingMode.DOWN);

            items.add(new ShareItem(i, amount, weight, exactAmount, calculatedAmount));
            distributedSum = distributedSum.add(calculatedAmount);
        }

        BigDecimal remainingToDistribute = distributeAmount.subtract(distributedSum);
        BigDecimal step = new BigDecimal("1").scaleByPowerOfTen(-scale);

        items.sort(Comparator.<ShareItem, BigDecimal>comparing(item -> item.remainder)
                .reversed()
                .thenComparingInt(item -> item.originalIndex));

        int itemIndex = 0;
        int failedAttempts = 0; // Защита от бесконечного цикла

        while (remainingToDistribute.compareTo(BigDecimal.ZERO) > 0 && itemIndex < items.size()) {
            ShareItem currentItem = items.get(itemIndex);
            
            if (!allowOverdraft) {
                if (currentItem.initialAmount.add(currentItem.calculatedAmount).add(step).compareTo(BigDecimal.ZERO) < 0) {
                   failedAttempts++;
                } else {
                   currentItem.calculatedAmount = currentItem.calculatedAmount.add(step);
                   remainingToDistribute = remainingToDistribute.subtract(step);
                   failedAttempts = 0;
                }
            } else {
                currentItem.calculatedAmount = currentItem.calculatedAmount.add(step);
                remainingToDistribute = remainingToDistribute.subtract(step);
                failedAttempts = 0;
            }

            itemIndex++;
            
            if (failedAttempts >= items.size()) {
                break;
            }

            if (itemIndex >= items.size() && remainingToDistribute.compareTo(BigDecimal.ZERO) > 0) {
                itemIndex = 0;
            }
        }

        items.sort(Comparator.comparingInt(item -> item.originalIndex));

        List<BigDecimal> finalResult = new ArrayList<>();
        for (ShareItem item : items) {
            finalResult.add(item.calculatedAmount);
        }

        return finalResult;
    }
}
