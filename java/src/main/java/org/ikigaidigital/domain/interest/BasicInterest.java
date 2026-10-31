package org.ikigaidigital.domain.interest;

/** Basic plan: 1% a year. */
public class BasicInterest implements InterestStrategy {

    @Override
    public String planType() {
        return "basic";
    }

    @Override
    public double interestAfterGracePeriod(double balance, int days) {
        return balance * 0.01 / 12;
    }
}
