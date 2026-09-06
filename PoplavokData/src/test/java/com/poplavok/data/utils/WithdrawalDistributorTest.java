package com.poplavok.data.utils;

import com.poplavok.data.utils.distributors.WithdrawalDistributor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WithdrawalDistributorTest {

    @Test
    void testExactTotalWithdrawal() {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                new BigDecimal("25.00")
        );
        BigDecimal total = new BigDecimal("175.00");

        List<BigDecimal> result = WithdrawalDistributor.distributeWithdrawal(amounts, total, 2, false);

        assertEquals(3, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0));
        assertEquals(new BigDecimal("50.00"), result.get(1));
        assertEquals(new BigDecimal("25.00"), result.get(2));
    }

    @Test
    void testPartialWithdrawal() {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("100.00"),
                new BigDecimal("50.00")
        );
        BigDecimal total = new BigDecimal("75.00"); // 50% withdrawal

        List<BigDecimal> result = WithdrawalDistributor.distributeWithdrawal(amounts, total, 2, false);

        assertEquals(2, result.size());
        // 75 / 150 = 0.50.
        // 100 * 0.50 = 50.00
        // 50 * 0.50 = 25.00
        assertEquals(new BigDecimal("50.00"), result.get(0));
        assertEquals(new BigDecimal("25.00"), result.get(1));
    }

    @Test
    void testPartialWithdrawalWithRoundingFloor() {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("100.00"),
                new BigDecimal("40.00")
        );
        BigDecimal total = new BigDecimal("47.00"); // sum is 140. 47 / 140 = 0.33571..
        // The largest-remainder allocation preserves the requested total.

        List<BigDecimal> result = WithdrawalDistributor.distributeWithdrawal(amounts, total, 2, false);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("33.57"), result.get(0));
        assertEquals(new BigDecimal("13.43"), result.get(1));
    }

    @Test
    void testWithdrawalGreaterThanSumThrowsException() {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        );
        BigDecimal total = new BigDecimal("25.00");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            WithdrawalDistributor.distributeWithdrawal(amounts, total, 2, false);
        });

        //assertEqual("Withdraw amount cannot be greater than the sum of all amounts.", exception.getMessage());
    }

    @Test
    void testZeroWithdrawalFromZeroSum() {
        List<BigDecimal> amounts = Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        List<BigDecimal> result = WithdrawalDistributor.distributeWithdrawal(amounts, total, 2, false);

        assertEquals(2, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0));
        assertEquals(BigDecimal.ZERO, result.get(1));
    }

    @Test
    void testZeroWithdrawalFromNonZeroSum() {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        );
        BigDecimal total = BigDecimal.ZERO;

        List<BigDecimal> result = WithdrawalDistributor.distributeWithdrawal(amounts, total, 2, false);

        assertEquals(2, result.size());
        // Ratio = 0.00
        assertEquals(new BigDecimal("0.00"), result.get(0));
        assertEquals(new BigDecimal("0.00"), result.get(1));
    }
}

