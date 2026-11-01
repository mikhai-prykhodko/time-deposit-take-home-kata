import { InterestStrategy } from './InterestStrategy'

/** Premium plan: 5% a year, starting after 45 days. */
export class PremiumInterest extends InterestStrategy {
  readonly planType = 'premium'

  protected interestAfterGracePeriod(balance: number, days: number): number {
    return days > 45 ? (balance * 0.05) / 12 : 0
  }
}
