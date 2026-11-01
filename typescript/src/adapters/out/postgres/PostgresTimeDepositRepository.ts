import { Pool, PoolClient } from 'pg'
import { TimeDeposit } from '../../../TimeDeposit'
import { TimeDepositDetails, Withdrawal } from '../../../application/TimeDepositDetails'
import { TimeDepositRepository } from '../../../application/ports/TimeDepositRepository'

type Queryable = Pool | PoolClient

interface DepositRow {
  id: number
  plan_type: string
  days: number
  balance: string
}

interface WithdrawalRow {
  id: number
  time_deposit_id: number
  amount: string
  date: string
}

/**
 * The domain uses JS numbers, the database NUMERIC(19,2). node-postgres returns NUMERIC as a string
 * (it cannot know whether a number fits a double), so values are converted here and only here:
 * read with Number(), written as text with two decimals.
 */
export class PostgresTimeDepositRepository implements TimeDepositRepository {
  constructor(private readonly db: Queryable) {}

  async findAll(): Promise<TimeDeposit[]> {
    const { rows } = await this.db.query<DepositRow>(
      'SELECT id, plan_type, days, balance FROM time_deposits ORDER BY id',
    )
    return rows.map(toDomain)
  }

  async findAllWithWithdrawals(): Promise<TimeDepositDetails[]> {
    const deposits = await this.findAll()
    // to_char keeps the date as written; letting pg build a JS Date would shift it by the time zone.
    const { rows } = await this.db.query<WithdrawalRow>(
      `SELECT id, time_deposit_id, amount, to_char(date, 'YYYY-MM-DD') AS date
       FROM withdrawals ORDER BY time_deposit_id, id`,
    )
    const byDeposit = new Map<number, Withdrawal[]>()
    for (const row of rows) {
      const list = byDeposit.get(row.time_deposit_id) ?? []
      list.push({ id: row.id, amount: Number(row.amount), date: row.date })
      byDeposit.set(row.time_deposit_id, list)
    }
    return deposits.map((deposit) => ({ deposit, withdrawals: byDeposit.get(deposit.id) ?? [] }))
  }

  async updateBalances(deposits: TimeDeposit[]): Promise<void> {
    if (deposits.length === 0) return
    // One statement for all deposits; ids not in the table are ignored.
    await this.db.query(
      `UPDATE time_deposits AS t SET balance = v.balance
       FROM unnest($1::int[], $2::numeric[]) AS v(id, balance)
       WHERE t.id = v.id`,
      [deposits.map((d) => d.id), deposits.map((d) => d.balance.toFixed(2))],
    )
  }
}

const toDomain = (row: DepositRow): TimeDeposit =>
  new TimeDeposit(row.id, row.plan_type, Number(row.balance), row.days)
