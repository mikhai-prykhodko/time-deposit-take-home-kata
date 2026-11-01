import { TimeDepositCalculator } from '../TimeDepositCalculator'
import { TransactionRunner } from './ports/TransactionRunner'
import { UpdateAllBalancesUseCase } from './ports/UpdateAllBalancesUseCase'

export class UpdateAllBalancesService implements UpdateAllBalancesUseCase {
  constructor(
    private readonly transactions: TransactionRunner,
    private readonly calculator: TimeDepositCalculator,
  ) {}

  /**
   * One transaction: either every balance is updated or none is.
   *
   * Assumption: simultaneous calls are not coordinated (concurrency control is out of scope), so two
   * overlapping updates may each read the same balances and apply a single month between them.
   */
  async updateAllBalances(): Promise<void> {
    await this.transactions.run(async (repository) => {
      const deposits = await repository.findAll()
      this.calculator.updateBalance(deposits)
      await repository.updateBalances(deposits)
    })
  }
}
