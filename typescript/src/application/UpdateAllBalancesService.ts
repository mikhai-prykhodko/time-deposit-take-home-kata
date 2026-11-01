import { Logger } from 'pino'
import { TimeDepositCalculator } from '../TimeDepositCalculator'
import { logger as defaultLogger } from '../logger'
import { TransactionRunner } from './ports/TransactionRunner'
import { UpdateAllBalancesUseCase } from './ports/UpdateAllBalancesUseCase'

export class UpdateAllBalancesService implements UpdateAllBalancesUseCase {
  constructor(
    private readonly transactions: TransactionRunner,
    private readonly calculator: TimeDepositCalculator,
    private readonly logger: Pick<Logger, 'info'> = defaultLogger,
  ) {}

  /**
   * One transaction: either every balance is updated or none is.
   *
   * Assumption: simultaneous calls are not coordinated (concurrency control is out of scope), so two
   * overlapping updates may each read the same balances and apply a single month between them.
   */
  async updateAllBalances(): Promise<void> {
    const startedAt = performance.now()
    const updated = await this.transactions.run(async (repository) => {
      const deposits = await repository.findAll()
      this.calculator.updateBalance(deposits)
      await repository.updateBalances(deposits)
      return deposits.length
    })
    this.logger.info(
      `Balance update finished: ${updated} time deposits updated in ${Math.round(performance.now() - startedAt)} ms`,
    )
  }
}
