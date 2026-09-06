package com.poplavok.data.utils.distributors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class SloppyDistributor implements Distributor {
    @Override
    public List<BigDecimal> distribute(List<BigDecimal> amounts, BigDecimal distributeAmount, int scale, boolean allowOverdraft) {
        List<BigDecimal> distributedAmounts = new ArrayList<>();

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) { sum = sum.add(amount); }

        if (!allowOverdraft && distributeAmount.compareTo(sum) > 0) {
            throw new RuntimeException("Distributed amount cannot be greater than the sum of all amounts, since overdraft is not enabled");
        }

        if (sum.compareTo(BigDecimal.ZERO) == 0) {
            if (distributeAmount.compareTo(BigDecimal.ZERO) == 0) {
                distributedAmounts.addAll(amounts);
            } else if (!amounts.isEmpty()) {
                // FALLBACK: When there is no sum to base proportions on, distribute evenly.
                BigDecimal equalShare = distributeAmount.divide(new BigDecimal(amounts.size()), scale, RoundingMode.FLOOR);
                for (int i = 0; i < amounts.size(); i++) {
                    distributedAmounts.add(equalShare);
                }
            }
            return distributedAmounts;
        }

        if (distributeAmount.compareTo(sum) == 0) {
            // 100% amounts distribution, just return the original amounts
            distributedAmounts.addAll(amounts);
        } else {
            BigDecimal ratio = distributeAmount.divide(sum, scale, RoundingMode.FLOOR);
            for (BigDecimal amount : amounts) {
                BigDecimal currentDistributedAmount = amount.multiply(ratio).setScale(scale, RoundingMode.FLOOR);
                distributedAmounts.add(currentDistributedAmount);
            }
        }

        return distributedAmounts;
    }
}
