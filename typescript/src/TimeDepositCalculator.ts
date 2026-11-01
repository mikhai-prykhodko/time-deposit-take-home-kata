import { TimeDeposit } from './TimeDeposit'
import { InterestStrategyRegistry } from './domain/interest/InterestStrategyRegistry'

export class TimeDepositCalculator {
  constructor(private readonly interestStrategies = InterestStrategyRegistry.withDefaultPlans()) {}

  public updateBalance(xs: TimeDeposit[]) {
    for (const deposit of xs) {
      const interest = this.interestStrategies.forPlan(deposit.planType).monthlyInterest(deposit.balance, deposit.days)

      // Same rounding as the original: Number.EPSILON lifts values like 1.015 (stored as 1.01499..) to the half cent.
      const roundedInterest = Math.round((interest + Number.EPSILON) * 100) / 100

      deposit.balance += roundedInterest
    }
  }
}
