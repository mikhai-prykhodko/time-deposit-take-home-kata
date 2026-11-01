import { BasicInterest } from './BasicInterest'
import { InterestStrategy } from './InterestStrategy'
import { PremiumInterest } from './PremiumInterest'
import { StudentInterest } from './StudentInterest'

/** Assumption: an unknown plan type earns no interest, as in the original calculator. */
class NoInterest extends InterestStrategy {
  // Stryker disable next-line StringLiteral: equivalent mutant, this strategy is never registered so its planType is never read
  readonly planType = ''

  protected interestAfterGracePeriod(): number {
    return 0
  }
}

const NO_INTEREST = new NoInterest()

/**
 * Finds the interest strategy for a plan type. A new plan is added by writing one
 * InterestStrategy and registering it; existing strategies are not touched.
 */
export class InterestStrategyRegistry {
  private readonly strategies = new Map<string, InterestStrategy>()

  constructor(strategies: InterestStrategy[]) {
    for (const strategy of strategies) {
      this.strategies.set(strategy.planType, strategy)
    }
  }

  static withDefaultPlans(): InterestStrategyRegistry {
    return new InterestStrategyRegistry([new BasicInterest(), new StudentInterest(), new PremiumInterest()])
  }

  forPlan(planType: string): InterestStrategy {
    return this.strategies.get(planType) ?? NO_INTEREST
  }
}
