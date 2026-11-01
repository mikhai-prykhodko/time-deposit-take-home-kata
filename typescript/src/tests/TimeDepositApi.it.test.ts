import jestOpenAPI from 'jest-openapi'
import { Express } from 'express'
import request from 'supertest'
import { OPENAPI_PATH } from '../adapters/in/http/createApp'
import { createApplication } from '../compositionRoot'
import { resetData, startTestDatabase, stopTestDatabase, TestDatabase } from './support/postgres'

/*
 * Whole application over HTTP with a real Postgres. Every response is also checked against openapi.yaml,
 * so the contract and the running service cannot drift apart.
 *
 * Expected balances are computed by hand with exact decimals, not with the application's own code, and
 * follow the TS rounding rule (no value below is a half-cent tie, so it agrees with plain half-up):
 *   basic   1234567.00 * 1% / 12 = 1028.81   -> 1235595.81, next month 1029.66 -> 1236625.47
 *   student    2000.00 * 3% / 12 = 5.00      -> 2005.00,    next month 5.01    -> 2010.01
 *   premium    5000.00 * 5% / 12 = 20.83     -> 5020.83,    next month 20.92   -> 5041.75
 */

jest.setTimeout(120_000)
jestOpenAPI(OPENAPI_PATH)

let db: TestDatabase
let app: Express

beforeAll(async () => {
  db = await startTestDatabase()
  app = createApplication(db.pool)
})

afterAll(() => stopTestDatabase(db))

beforeEach(async () => {
  await resetData(db.pool)
  await db.pool.query(`INSERT INTO time_deposits (id, plan_type, days, balance) VALUES
    (1, 'basic', 45, 1234567.00), (2, 'student', 100, 2000.00), (3, 'premium', 100, 5000.00),
    (4, 'basic', 20, 1500.00), (5, 'student', 400, 3000.00), (6, 'premium', 40, 8000.00)`)
  await db.pool.query(`INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
    (1, 1, 100.00, '2024-01-15'), (2, 1, 50.25, '2024-02-20')`)
})

const getAll = async () => {
  const response = await request(app).get('/time-deposits')
  expect(response.status).toBe(200)
  expect(response).toSatisfyApiSpec()
  return response.body
}

const updateAll = async () => {
  const response = await request(app).post('/time-deposits/balance-updates')
  expect(response.status).toBe(204)
  expect(response).toSatisfyApiSpec()
}

describe('Time deposit API end to end', () => {
  test('GET returns all deposits with their withdrawals', async () => {
    const body = await getAll()

    expect(body.map((d: { id: number }) => d.id)).toEqual([1, 2, 3, 4, 5, 6])
    expect(body[0]).toEqual({
      id: 1,
      planType: 'basic',
      balance: 1234567,
      days: 45,
      withdrawals: [
        { id: 1, amount: 100, date: '2024-01-15' },
        { id: 2, amount: 50.25, date: '2024-02-20' },
      ],
    })
    expect(body[1].withdrawals).toEqual([])
  })

  test('POST applies one month of interest and GET shows the new balances', async () => {
    await updateAll()

    const body = await getAll()
    expect(body.map((d: { balance: number }) => d.balance)).toEqual([1235595.81, 2005, 5020.83, 1500, 3000, 8000])
    // only balances move: plan, age and withdrawals stay as they were
    expect(body.map((d: { planType: string }) => d.planType)).toEqual(['basic', 'student', 'premium', 'basic', 'student', 'premium'])
    expect(body.map((d: { days: number }) => d.days)).toEqual([45, 100, 100, 20, 400, 40])
    expect(body[0].withdrawals).toHaveLength(2)
  })

  test('each POST applies another month', async () => {
    await updateAll()
    await updateAll()

    const body = await getAll()
    expect(body.map((d: { balance: number }) => d.balance)).toEqual([1236625.47, 2010.01, 5041.75, 1500, 3000, 8000])
  })

  test('POST on an empty database succeeds and GET returns an empty list', async () => {
    await resetData(db.pool)

    await updateAll()
    expect(await getAll()).toEqual([])
  })
})
