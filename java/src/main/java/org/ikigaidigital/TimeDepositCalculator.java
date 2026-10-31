package org.ikigaidigital;

import org.ikigaidigital.domain.interest.InterestStrategyRegistry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TimeDepositCalculator {
    private final InterestStrategyRegistry interestStrategies;

    public TimeDepositCalculator() {
        this(InterestStrategyRegistry.withDefaultPlans());
    }

    public TimeDepositCalculator(InterestStrategyRegistry interestStrategies) {
        this.interestStrategies = interestStrategies;
    }

    public void updateBalance(List<TimeDeposit> xs) {
        for (TimeDeposit deposit : xs) {
            double interest = interestStrategies.forPlan(deposit.getPlanType())
                .monthlyInterest(deposit.getBalance(), deposit.getDays());

            // Rounds the exact double value (not its decimal text), as the original code did.
            double roundedInterest = new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).doubleValue();
            deposit.setBalance(deposit.getBalance() + roundedInterest);
        }
    }
}
