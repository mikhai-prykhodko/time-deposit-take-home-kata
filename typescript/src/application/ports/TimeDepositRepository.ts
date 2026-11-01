import { TimeDeposit } from '../../TimeDeposit'
import { TimeDepositDetails } from '../TimeDepositDetails'

/** Deposits, and each deposit's withdrawals, are returned ordered by id so responses are stable. */
export interface TimeDepositRepository {
  findAll(): Promise<TimeDeposit[]>

  findAllWithWithdrawals(): Promise<TimeDepositDetails[]>

  /** Stores the current balance of each given deposit; nothing else about a deposit changes. */
  updateBalances(deposits: TimeDeposit[]): Promise<void>
}
