import { TimeDepositRepository } from './TimeDepositRepository'

/**
 * Runs work in one transaction: everything it does through the given repository is committed
 * together, or rolled back together if it throws. (The Java solution gets this from @Transactional.)
 */
export interface TransactionRunner {
  run<T>(work: (repository: TimeDepositRepository) => Promise<T>): Promise<T>
}
