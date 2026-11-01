import { BasicInterest } from './BasicInterest'
import { InterestStrategy } from './InterestStrategy'
import { PremiumInterest } from './PremiumInterest'
import { StudentInterest } from './StudentInterest'

/** Assumption: an unknown plan type earns no interest, as in the original calculator. */
class NoInterest extends InterestStrategy {
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
  private readonly strategies: Map<string, InterestStrategy>

  constructor(strategies: InterestStrategy[]) {
    this.strategies = new Map(strategies.map((strategy) => [strategy.planType, strategy]))
  }

  static withDefaultPlans(): InterestStrategyRegistry {
    return new InterestStrategyRegistry([new BasicInterest(), new StudentInterest(), new PremiumInterest()])
  }

  forPlan(planType: string): InterestStrategy {
    return this.strategies.get(planType) ?? NO_INTEREST
  }
}
