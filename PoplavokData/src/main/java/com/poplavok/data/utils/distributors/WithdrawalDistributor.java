package com.poplavok.data.utils.distributors;

import java.math.BigDecimal;
import java.util.List;

// TODO: dummy implementation, only stubs "100% withdrawal" use case, need to implement the proper distribution logic
public class WithdrawalDistributor {
    static final Distributor DISTRIBUTOR = new PreciseDistributor();

    public static List<BigDecimal> distributeWithdrawal(List<BigDecimal> amounts, BigDecimal withdrawalAmount, int scale) {
        return DISTRIBUTOR.distribute(amounts, withdrawalAmount, scale);
    }

    public static List<BigDecimal> distributeWithdrawal(List<BigDecimal> amounts, BigDecimal withdrawalAmount, int scale, boolean allowOverdraft) {
        return DISTRIBUTOR.distribute(amounts, withdrawalAmount, scale, allowOverdraft);
    }
}
