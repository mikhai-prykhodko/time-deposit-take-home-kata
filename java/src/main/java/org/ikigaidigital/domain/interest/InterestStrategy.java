package org.ikigaidigital.domain.interest;

/**
 * Interest rule of one plan type. The grace period common to all plans lives here,
 * so a plan only describes what is specific to it.
 */
public interface InterestStrategy {

    /** No plan earns interest during the first 30 days. */
    int GRACE_PERIOD_DAYS = 30;

    /** The plan type this strategy applies to, as stored on a time deposit. */
    String planType();

    /** Unrounded interest for one month on a deposit that is past the grace period. */
    double interestAfterGracePeriod(double balance, int days);

    default double monthlyInterest(double balance, int days) {
        return days > GRACE_PERIOD_DAYS ? interestAfterGracePeriod(balance, days) : 0;
    }
}
