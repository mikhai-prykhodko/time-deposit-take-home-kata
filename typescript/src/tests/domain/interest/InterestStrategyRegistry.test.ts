import { BasicInterest } from '../../../domain/interest/BasicInterest'
import { InterestStrategyRegistry } from '../../../domain/interest/InterestStrategyRegistry'
import { PremiumInterest } from '../../../domain/interest/PremiumInterest'
import { StudentInterest } from '../../../domain/interest/StudentInterest'

const registry = InterestStrategyRegistry.withDefaultPlans()

describe('InterestStrategyRegistry', () => {
  test.each([
    ['basic', BasicInterest],
    ['student', StudentInterest],
    ['premium', PremiumInterest],
  ])('resolves the strategy of the %s plan', (planType, strategyClass) => {
    expect(registry.forPlan(planType)).toBeInstanceOf(strategyClass)
  })

  test('an unknown plan earns no interest', () => {
    expect(registry.forPlan('gold').monthlyInterest(1200, 1000)).toBe(0)
  })

  test.each([
    ['basic', 30, 0],
    ['basic', 31, 1],
    ['student', 365, 3],
    ['student', 366, 0],
    ['premium', 45, 0],
    ['premium', 46, 5],
  ])('%s plan, %i days: unrounded monthly interest on 1200 is %d', (planType, days, expected) => {
    expect(registry.forPlan(planType).monthlyInterest(1200, days)).toBeCloseTo(expected, 10)
  })
})
