import { TimeDeposit } from '../../TimeDeposit'
import { TimeDepositDetails, Withdrawal } from '../../application/TimeDepositDetails'
import { TimeDepositRepository } from '../../application/ports/TimeDepositRepository'
import { TransactionRunner } from '../../application/ports/TransactionRunner'

/** Fake ports for fast use case tests: no Express, no database. */
export class InMemoryTimeDepositRepository implements TimeDepositRepository, TransactionRunner {
  private readonly deposits = new Map<number, TimeDeposit>()
  private readonly withdrawals = new Map<number, Withdrawal[]>()

  add(deposit: TimeDeposit, ...withdrawals: Withdrawal[]): this {
    this.deposits.set(deposit.id, deposit)
    this.withdrawals.set(deposit.id, withdrawals)
    return this
  }

  async findAll(): Promise<TimeDeposit[]> {
    // copies, like a real database: changing them does not change stored data until updateBalances
    return [...this.deposits.values()]
      .sort((a, b) => a.id - b.id)
      .map((d) => new TimeDeposit(d.id, d.planType, d.balance, d.days))
  }

  async findAllWithWithdrawals(): Promise<TimeDepositDetails[]> {
    return (await this.findAll()).map((deposit) => ({ deposit, withdrawals: this.withdrawals.get(deposit.id) ?? [] }))
  }

  async updateBalances(updated: TimeDeposit[]): Promise<void> {
    for (const deposit of updated) this.deposits.get(deposit.id)!.balance = deposit.balance
  }

  run<T>(work: (repository: TimeDepositRepository) => Promise<T>): Promise<T> {
    return work(this)
  }

  balanceOf(id: number): number {
    return this.deposits.get(id)!.balance
  }
}
