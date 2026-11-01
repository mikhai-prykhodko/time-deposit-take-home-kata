import { TimeDeposit } from '../../../../TimeDeposit'
import { PostgresTimeDepositRepository } from '../../../../adapters/out/postgres/PostgresTimeDepositRepository'
import { PostgresTransactionRunner } from '../../../../adapters/out/postgres/PostgresTransactionRunner'
import { resetData, startTestDatabase, stopTestDatabase, TestDatabase } from '../../../support/postgres'

jest.setTimeout(120_000)

let db: TestDatabase
let repository: PostgresTimeDepositRepository

beforeAll(async () => {
  db = await startTestDatabase()
  repository = new PostgresTimeDepositRepository(db.pool)
})

afterAll(() => stopTestDatabase(db))

beforeEach(async () => {
  await resetData(db.pool)
  await db.pool.query(`INSERT INTO time_deposits (id, plan_type, days, balance) VALUES
    (2, 'student', 100, 2000.00), (1, 'basic', 45, 1234567.00)`)
  await db.pool.query(`INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
    (11, 1, 50.25, '2024-02-20'), (10, 1, 100.00, '2024-01-15')`)
})

const balanceOf = async (id: number): Promise<string> =>
  (await db.pool.query('SELECT balance FROM time_deposits WHERE id = $1', [id])).rows[0].balance

describe('PostgresTimeDepositRepository (real Postgres)', () => {
  test('findAll returns domain deposits ordered by id, with numeric balances', async () => {
    const deposits = await repository.findAll()

    expect(deposits).toEqual([
      new TimeDeposit(1, 'basic', 1234567, 45),
      new TimeDeposit(2, 'student', 2000, 100),
    ])
  })

  test('findAllWithWithdrawals attaches withdrawals ordered by id, dates as written', async () => {
    const details = await repository.findAllWithWithdrawals()

    expect(details.map((d) => d.deposit.id)).toEqual([1, 2])
    expect(details[0].withdrawals).toEqual([
      { id: 10, amount: 100, date: '2024-01-15' },
      { id: 11, amount: 50.25, date: '2024-02-20' },
    ])
    expect(details[1].withdrawals).toEqual([])
  })

  test('updateBalances stores the new balance without touching other deposits or columns', async () => {
    await repository.updateBalances([new TimeDeposit(1, 'basic', 1235595.81, 45)])

    expect(await balanceOf(1)).toBe('1235595.81')
    expect(await balanceOf(2)).toBe('2000.00')
    const { rows } = await db.pool.query('SELECT plan_type, days FROM time_deposits WHERE id = 1')
    expect(rows[0]).toEqual({ plan_type: 'basic', days: 45 })
  })

  test('balance survives the number to NUMERIC round trip without drift', async () => {
    await repository.updateBalances([new TimeDeposit(2, 'student', 2002.5, 100)])

    expect((await repository.findAll()).map((d) => d.balance)).toEqual([1234567, 2002.5])
  })
})

describe('PostgresTransactionRunner (real Postgres)', () => {
  test('commits the work when it succeeds', async () => {
    await new PostgresTransactionRunner(db.pool).run((repo) =>
      repo.updateBalances([new TimeDeposit(2, 'student', 2005, 100)]),
    )

    expect(await balanceOf(2)).toBe('2005.00')
  })

  test('rolls everything back when the work fails', async () => {
    const failing = new PostgresTransactionRunner(db.pool).run(async (repo) => {
      await repo.updateBalances([new TimeDeposit(2, 'student', 9999, 100)])
      throw new Error('boom')
    })

    await expect(failing).rejects.toThrow('boom')
    expect(await balanceOf(2)).toBe('2000.00')
  })
})
