package org.ikigaidigital.domain.interest;

/** Student plan: 3% a year, none once the deposit is a year old (366 days or more). */
public class StudentInterest implements InterestStrategy {

    @Override
    public String planType() {
        return "student";
    }

    @Override
    public double interestAfterGracePeriod(double balance, int days) {
        return days < 366 ? balance * 0.03 / 12 : 0;
    }
}
