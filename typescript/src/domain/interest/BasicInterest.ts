import { InterestStrategy } from './InterestStrategy'

/** Basic plan: 1% a year. */
export class BasicInterest extends InterestStrategy {
  readonly planType = 'basic'

  protected interestAfterGracePeriod(balance: number): number {
    return (balance * 0.01) / 12
  }
}
