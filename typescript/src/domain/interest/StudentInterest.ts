import { InterestStrategy } from './InterestStrategy'

/** Student plan: 3% a year, none once the deposit is a year old (366 days or more). */
export class StudentInterest extends InterestStrategy {
  readonly planType = 'student'

  protected interestAfterGracePeriod(balance: number, days: number): number {
    return days < 366 ? (balance * 0.03) / 12 : 0
  }
}
