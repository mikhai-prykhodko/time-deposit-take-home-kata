package org.ikigaidigital.domain.interest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class InterestStrategyRegistryTest {

    private final InterestStrategyRegistry registry = InterestStrategyRegistry.withDefaultPlans();

    @ParameterizedTest
    @CsvSource({"basic,BasicInterest", "student,StudentInterest", "premium,PremiumInterest"})
    void resolvesTheStrategyOfEachDefaultPlan(String planType, String strategyName) {
        assertThat(registry.forPlan(planType).getClass().getSimpleName()).isEqualTo(strategyName);
    }

    @Test
    void unknownPlanEarnsNoInterest() {
        assertThat(registry.forPlan("gold").monthlyInterest(1200.00, 1000)).isZero();
    }

    @ParameterizedTest(name = "{0}: {1} days -> {2}")
    @CsvSource({
        "basic,   30, 0",
        "basic,   31, 1.0",
        "student, 365, 3.0",
        "student, 366, 0",
        "premium, 45, 0",
        "premium, 46, 5.0",
    })
    void returnsUnroundedMonthlyInterestOnA1200Balance(String planType, int days, double expected) {
        assertThat(registry.forPlan(planType).monthlyInterest(1200.00, days)).isEqualTo(expected);
    }
}
