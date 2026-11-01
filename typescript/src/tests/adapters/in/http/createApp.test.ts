import request from 'supertest'
import { TimeDeposit } from '../../../../TimeDeposit'
import { createApp } from '../../../../adapters/in/http/createApp'

const listAll = jest.fn()
const updateAllBalances = jest.fn()
const app = createApp({ listTimeDeposits: { listAll }, updateAllBalances: { updateAllBalances } })

beforeEach(() => jest.resetAllMocks())

describe('HTTP adapter', () => {
  test('GET returns every deposit with the agreed fields', async () => {
    listAll.mockResolvedValue([
      {
        deposit: new TimeDeposit(1, 'basic', 1000.5, 45),
        withdrawals: [{ id: 7, amount: 100, date: '2024-01-15' }],
      },
      { deposit: new TimeDeposit(2, 'premium', 5000, 100), withdrawals: [] },
    ])

    const response = await request(app).get('/time-deposits')

    expect(response.status).toBe(200)
    expect(response.headers['content-type']).toMatch(/application\/json/)
    expect(response.body).toEqual([
      { id: 1, planType: 'basic', balance: 1000.5, days: 45, withdrawals: [{ id: 7, amount: 100, date: '2024-01-15' }] },
      { id: 2, planType: 'premium', balance: 5000, days: 100, withdrawals: [] },
    ])
  })

  test('POST triggers the balance update and returns 204 with no body', async () => {
    updateAllBalances.mockResolvedValue(undefined)

    const response = await request(app).post('/time-deposits/balance-updates')

    expect(response.status).toBe(204)
    expect(response.text).toBe('')
    expect(updateAllBalances).toHaveBeenCalledTimes(1)
  })

  test('the application exposes exactly two API endpoints', () => {
    // Every route registered on the app, except the contract document served for Swagger UI.
    const documentation = ['GET /api-docs']
    const routes = (app.router.stack as any[])
      .filter((layer) => layer.route)
      .flatMap((layer) => Object.keys(layer.route.methods).map((m) => `${m.toUpperCase()} ${layer.route.path}`))
      .filter((route) => !documentation.includes(route))

    expect(routes.sort()).toEqual(['GET /time-deposits', 'POST /time-deposits/balance-updates'])
  })
})
