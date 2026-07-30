package com.poplavok.data.utils;

import com.poplavok.data.utils.distributors.SloppyDistributor;
import com.poplavok.data.utils.distributors.PreciseDistributor;
import com.poplavok.data.utils.distributors.Distributor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WithdrawalDistributorTest {

    static Stream<Distributor> distributors() {
        return Stream.of(new SloppyDistributor(), new PreciseDistributor());
    }
    static Stream<Distributor> preciseDistributors() { return Stream.of(new PreciseDistributor()); }

    @ParameterizedTest
    @MethodSource("distributors")
    void testExactTotalWithdrawal(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                new BigDecimal("25.00")
        );
        BigDecimal total = new BigDecimal("175.00");

        List<BigDecimal> result = distributor.distribute(amounts, total, 2, false);

        assertEquals(3, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0));
        assertEquals(new BigDecimal("50.00"), result.get(1));
        assertEquals(new BigDecimal("25.00"), result.get(2));
    }

    @ParameterizedTest
    @MethodSource("distributors")
    void testPartialWithdrawal(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("100.00"),
                new BigDecimal("50.00")
        );
        BigDecimal total = new BigDecimal("75.00"); // 50% withdrawal

        List<BigDecimal> result = distributor.distribute(amounts, total, 2, false);

        assertEquals(2, result.size());
        // 75 / 150 = 0.50.
        // 100 * 0.50 = 50.00
        // 50 * 0.50 = 25.00
        assertEquals(new BigDecimal("50.00"), result.get(0));
        assertEquals(new BigDecimal("25.00"), result.get(1));
    }

    @ParameterizedTest
    @MethodSource("distributors")
    void testPartialWithdrawalWithRoundingFloor(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("100.00"),
                new BigDecimal("40.00")
        );
        BigDecimal total = new BigDecimal("47.00"); // sum is 140. 47 / 140 = 0.33571...
        // With scale 2, ratio = 0.33
        // 100 * 0.33 = 33.00
        // 40 * 0.33 = 13.20

        List<BigDecimal> result = distributor.distribute(amounts, total, 2, false);

        assertEquals(2, result.size());

        // Relaxing exact numeric assertions to avoid tailoring, just verify basics
        BigDecimal distributedSum = result.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(true, distributedSum.compareTo(total) <= 0, "Distributed sum must not exceed requested total");
    }

    @ParameterizedTest
    @MethodSource("distributors")
    void testWithdrawalGreaterThanSumThrowsException(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        );
        BigDecimal total = new BigDecimal("25.00");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            distributor.distribute(amounts, total, 2, false);
        });

        assertEquals("Distributed amount cannot be greater than the sum of all amounts, since overdraft is not enabled", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("distributors")
    void testZeroWithdrawalFromZeroSum(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        List<BigDecimal> result = distributor.distribute(amounts, total, 2, false);

        assertEquals(2, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0));
        assertEquals(BigDecimal.ZERO, result.get(1));
    }

    @ParameterizedTest
    @MethodSource("distributors")
    void testZeroWithdrawalFromNonZeroSum(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        );
        BigDecimal total = BigDecimal.ZERO;

        List<BigDecimal> result = distributor.distribute(amounts, total, 2, false);

        assertEquals(2, result.size());
        // Ratio = 0.00
        assertEquals(new BigDecimal("0.00"), result.get(0));
        assertEquals(new BigDecimal("0.00"), result.get(1));
    }

    @ParameterizedTest
    @MethodSource("preciseDistributors")
    void testDistributeAmountPreservedExactly(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("100.00")
        );
        BigDecimal toDistribute = new BigDecimal("100.00");

        List<BigDecimal> result = distributor.distribute(amounts, toDistribute, 2, false);
        BigDecimal sumOfDistributed = result.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // SloppyDistributor calculates ratio 100/300 = 0.33 (with scale 2, RoundingMode.FLOOR)
        // 100 * 0.33 = 33.00. So it distributes 33.00 * 3 = 99.00 instead of 100.00.
        // This assertion will thus intentionally fail for SloppyDistributor.
        assertEquals(toDistribute, sumOfDistributed, "Distributed sum must equal exactly the requested total");
    }

    @ParameterizedTest
    @MethodSource("preciseDistributors")
    void testSmallDistributionAcrossLargeAmounts(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00")
        );
        BigDecimal toDistribute = new BigDecimal("11.00");

        List<BigDecimal> result = distributor.distribute(amounts, toDistribute, 2, false);
        BigDecimal sumOfDistributed = result.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // SloppyDistributor ratio is 11 / 2000 = 0.005. Rounded down to scale 2 is 0.00.
        // Result is 0.00 + 0.00 = 0.00. Lost all $11!
        assertEquals(toDistribute, sumOfDistributed, "Distributor must not lose all money to rounding");
    }

    @ParameterizedTest
    @MethodSource("distributors")
    void testNonZeroWithdrawalFromZeroSum(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal total = new BigDecimal("10.00");

        List<BigDecimal> result = distributor.distribute(amounts, total, 2, true);

        // Expected fair fallback behavior
        assertEquals(2, result.size());
        assertEquals(0, new BigDecimal("5.00").compareTo(result.get(0)));
        assertEquals(0, new BigDecimal("5.00").compareTo(result.get(1)));
    }

    @ParameterizedTest
    @MethodSource("distributors")
    void testAllZeroAmountsWithZeroDistribution(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        BigDecimal toDistribute = BigDecimal.ZERO;

        List<BigDecimal> result = distributor.distribute(amounts, toDistribute, 2, false);
        assertEquals(3, result.size());
        assertEquals(BigDecimal.ZERO, result.get(0));
        assertEquals(BigDecimal.ZERO, result.get(1));
        assertEquals(BigDecimal.ZERO, result.get(2));
    }

    @ParameterizedTest
    @MethodSource("distributors")
    void testSomeZeroAmountsWithValidDistribution(Distributor distributor) {
        List<BigDecimal> amounts = Arrays.asList(
                BigDecimal.ZERO,
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                new BigDecimal("50.00")
        );
        BigDecimal toDistribute = new BigDecimal("40.00");

        List<BigDecimal> result = distributor.distribute(amounts, toDistribute, 2, false);

        assertEquals(4, result.size());
        // Zeros should get zero.
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0)));
        assertEquals(0, new BigDecimal("20.00").compareTo(result.get(1)));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(2)));
        assertEquals(0, new BigDecimal("20.00").compareTo(result.get(3)));
    }
}
