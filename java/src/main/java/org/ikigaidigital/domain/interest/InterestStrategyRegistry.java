package org.ikigaidigital.domain.interest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds the interest strategy for a plan type. A new plan is added by writing one
 * InterestStrategy and registering it; existing strategies are not touched.
 */
public class InterestStrategyRegistry {

    /** Assumption: an unknown plan type earns no interest, as in the original calculator. */
    private static final InterestStrategy NO_INTEREST = new InterestStrategy() {
        @Override
        public String planType() {
            return "";
        }

        @Override
        public double interestAfterGracePeriod(double balance, int days) {
            return 0;
        }
    };

    private final Map<String, InterestStrategy> strategies = new HashMap<>();

    public InterestStrategyRegistry(Collection<? extends InterestStrategy> strategies) {
        strategies.forEach(strategy -> this.strategies.put(strategy.planType(), strategy));
    }

    public static InterestStrategyRegistry withDefaultPlans() {
        return new InterestStrategyRegistry(List.of(
            new BasicInterest(), new StudentInterest(), new PremiumInterest()));
    }

    public InterestStrategy forPlan(String planType) {
        return strategies.getOrDefault(planType, NO_INTEREST);
    }
}
