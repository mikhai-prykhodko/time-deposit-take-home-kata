/**
 * Interest rule of one plan type. The grace period common to all plans lives here,
 * so a plan only describes what is specific to it.
 *
 * Assumptions, taken from the original calculator (which the assignment calls correct):
 * the stated rates are annual and one update applies a twelfth of the rate; "no interest for
 * the first 30 days" means the first interest is earned at day 31; plan types match exactly
 * and in lower case.
 */
export abstract class InterestStrategy {
  /** No plan earns interest during the first 30 days. */
  static readonly GRACE_PERIOD_DAYS = 30

  /** The plan type this strategy applies to, as stored on a time deposit. */
  abstract readonly planType: string

  /** Unrounded interest for one month on a deposit that is past the grace period. */
  protected abstract interestAfterGracePeriod(balance: number, days: number): number

  monthlyInterest(balance: number, days: number): number {
    return days > InterestStrategy.GRACE_PERIOD_DAYS ? this.interestAfterGracePeriod(balance, days) : 0
  }
}
