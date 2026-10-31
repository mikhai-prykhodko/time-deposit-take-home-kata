package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization tests: they pin the behaviour of the original updateBalance
 * implementation, which the README states is correct. Expected values were
 * produced by the unmodified code. Do not edit these tests to make a refactor pass.
 *
 * Monthly interest = balance * annualRate / 12, rounded to 2 decimals HALF_UP
 * via new BigDecimal(double), then added to the balance.
 */
public class TimeDepositCalculatorTest {

    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0} plan, {1} days: {2} -> {3}")
    @CsvSource({
        // basic: 1% p.a. after the first 30 days -> 1200 * 0.01 / 12 = 1.00
        "basic,     30, 1200.00, 1200.00",
        "basic,     31, 1200.00, 1201.00",
        "basic,   1000, 1200.00, 1201.00",
        // student: 3% p.a. from day 31 while days < 366 -> 1200 * 0.03 / 12 = 3.00
        "student,   30, 1200.00, 1200.00",
        "student,   31, 1200.00, 1203.00",
        "student,  365, 1200.00, 1203.00",
        "student,  366, 1200.00, 1200.00",
        // premium: 5% p.a., only when days > 45 -> 1200 * 0.05 / 12 = 5.00
        "premium,   30, 1200.00, 1200.00",
        "premium,   45, 1200.00, 1200.00",
        "premium,   46, 1200.00, 1205.00",
    })
    void appliesMonthlyInterestByPlanAndDayBoundaries(String planType, int days, double balance, double expected) {
        assertThat(updatedBalance(planType, days, balance)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "plan ''{0}'' earns no interest")
    @CsvSource({"gold", "Basic", "PREMIUM", "''"})
    void unknownOrDifferentlyCasedPlanTypeEarnsNoInterest(String planType) {
        assertThat(updatedBalance(planType, 100, 1200.00)).isEqualTo(1200.00);
    }

    @ParameterizedTest(name = "{0} plan: {1} -> {2}")
    @CsvSource({
        // 1234567 * 0.01 / 12 = 1028.8058.. -> 1028.81
        "basic,   1234567.00, 1235595.81",
        // 1000 * 0.03 / 12 = 2.50 exactly
        "student,    1000.00,    1002.50",
        // 1000 * 0.05 / 12 = 4.1666.. -> 4.17
        "premium,    1000.00,    1004.17",
        // 10.01 * 0.01 / 12 = 0.00834.. -> 0.01
        "basic,        10.01,      10.02",
        // 1 * 0.05 / 12 = 0.00416.. -> 0.00
        "premium,       1.00,       1.00",
        "basic,         0.00,       0.00",
    })
    void roundsInterestToTwoDecimals(String planType, double balance, double expected) {
        assertThat(updatedBalance(planType, 100, balance)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "basic plan: {0} -> {1}")
    @CsvSource({
        // 1206 * 0.01 / 12 is 1.005 as a double slightly above the half -> 1.01
        "1206.00, 1207.01",
        // 1218 * 0.01 / 12 is 1.015 as a double slightly BELOW the half -> 1.01, not 1.02.
        // This pins the new BigDecimal(double) rounding path; BigDecimal.valueOf would give 1.02.
        "1218.00, 1219.01",
    })
    void halfCentInterestRoundsOnTheExactDoubleValue(double balance, double expected) {
        assertThat(updatedBalance("basic", 100, balance)).isEqualTo(expected);
    }

    @Test
    void updatesEveryDepositInTheListIndependently() {
        List<TimeDeposit> deposits = List.of(
            new TimeDeposit(1, "basic", 1200.00, 31),
            new TimeDeposit(2, "student", 1200.00, 366),
            new TimeDeposit(3, "premium", 1200.00, 46),
            new TimeDeposit(4, "gold", 1200.00, 100)
        );

        calculator.updateBalance(deposits);

        assertThat(deposits).extracting(TimeDeposit::getBalance)
            .containsExactly(1201.00, 1200.00, 1205.00, 1200.00);
    }

    @Test
    void changesOnlyTheBalance() {
        TimeDeposit deposit = new TimeDeposit(7, "premium", 1200.00, 46);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getId()).isEqualTo(7);
        assertThat(deposit.getPlanType()).isEqualTo("premium");
        assertThat(deposit.getDays()).isEqualTo(46);
    }

    @Test
    void appliesOneMonthOfInterestPerCall() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1200.00, 31);

        calculator.updateBalance(List.of(deposit));
        calculator.updateBalance(List.of(deposit));

        // 1200 -> 1201.00 -> 1201 * 0.01 / 12 = 1.0008.. -> 1.00 -> 1202.00
        assertThat(deposit.getBalance()).isEqualTo(1202.00);
    }

    @Test
    void emptyListIsANoOp() {
        List<TimeDeposit> deposits = new ArrayList<>();

        calculator.updateBalance(deposits);

        assertThat(deposits).isEmpty();
    }

    private double updatedBalance(String planType, int days, double balance) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        calculator.updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}
