package org.ikigaidigital.domain.interest;

/** Premium plan: 5% a year, starting after 45 days. */
public class PremiumInterest implements InterestStrategy {

    @Override
    public String planType() {
        return "premium";
    }

    @Override
    public double interestAfterGracePeriod(double balance, int days) {
        return days > 45 ? balance * 0.05 / 12 : 0;
    }
}
