import { TimeDeposit } from '../../../TimeDeposit'
import { TimeDepositCalculator } from '../../../TimeDepositCalculator'
import { BasicInterest } from '../../../domain/interest/BasicInterest'
import { InterestStrategy } from '../../../domain/interest/InterestStrategy'
import { InterestStrategyRegistry } from '../../../domain/interest/InterestStrategyRegistry'
import { PremiumInterest } from '../../../domain/interest/PremiumInterest'
import { StudentInterest } from '../../../domain/interest/StudentInterest'

/** Hypothetical plan, only for this test: 2% a year, from day 31 like every plan. */
class GoldInterest extends InterestStrategy {
  readonly planType = 'gold'

  protected interestAfterGracePeriod(balance: number): number {
    return (balance * 0.02) / 12
  }
}

/** Shows the open/closed goal: a new plan is one new class plus registration. */
test('a new plan earns its own interest without changing existing plans', () => {
  const registry = new InterestStrategyRegistry([
    new BasicInterest(),
    new StudentInterest(),
    new PremiumInterest(),
    new GoldInterest(),
  ])
  const deposits = [
    new TimeDeposit(1, 'gold', 1200, 31),
    new TimeDeposit(2, 'gold', 1200, 30),
    new TimeDeposit(3, 'basic', 1200, 31),
  ]

  new TimeDepositCalculator(registry).updateBalance(deposits)

  // gold: 1200 * 0.02 / 12 = 2.00; the grace period still applies; basic is unchanged
  expect(deposits.map((d) => d.balance)).toEqual([1202, 1200, 1201])
})
