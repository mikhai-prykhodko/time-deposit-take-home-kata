package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Shows the open/closed goal: a new plan is one new class plus registration. */
class NewPlanExtensibilityTest {

    /** Hypothetical plan, only for this test: 2% a year, from day 31 like every plan. */
    private static class GoldInterest implements InterestStrategy {
        @Override
        public String planType() {
            return "gold";
        }

        @Override
        public double interestAfterGracePeriod(double balance, int days) {
            return balance * 0.02 / 12;
        }
    }

    @Test
    void newPlanEarnsItsOwnInterestWithoutChangingExistingPlans() {
        List<InterestStrategy> strategies = new ArrayList<>(List.of(
            new BasicInterest(), new StudentInterest(), new PremiumInterest()));
        strategies.add(new GoldInterest());
        TimeDepositCalculator calculator = new TimeDepositCalculator(new InterestStrategyRegistry(strategies));
        List<TimeDeposit> deposits = List.of(
            new TimeDeposit(1, "gold", 1200.00, 31),
            new TimeDeposit(2, "gold", 1200.00, 30),
            new TimeDeposit(3, "basic", 1200.00, 31));

        calculator.updateBalance(deposits);

        // gold: 1200 * 0.02 / 12 = 2.00; grace period still applies; basic is unchanged.
        assertThat(deposits).extracting(TimeDeposit::getBalance)
            .containsExactly(1202.00, 1200.00, 1201.00);
    }
}
