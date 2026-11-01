import express, { Express, Request, Response } from 'express'
import { readFileSync } from 'fs'
import { join } from 'path'
import swaggerUi from 'swagger-ui-express'
import { parse } from 'yaml'
import type { components } from '../../../generated/api'
import { TimeDepositDetails } from '../../../application/TimeDepositDetails'
import { ListTimeDepositsUseCase } from '../../../application/ports/ListTimeDepositsUseCase'
import { UpdateAllBalancesUseCase } from '../../../application/ports/UpdateAllBalancesUseCase'

type TimeDepositResponse = components['schemas']['TimeDepositResponse']

export const OPENAPI_PATH = join(__dirname, '../../../../openapi/openapi.yaml')

export interface UseCases {
  listTimeDeposits: ListTimeDepositsUseCase
  updateAllBalances: UpdateAllBalancesUseCase
}

/**
 * The two API routes, with paths and response shapes taken from openapi.yaml (the response type is
 * generated from it), plus the Swagger UI and raw contract, which document the API and are not part of it.
 */
export const createApp = ({ listTimeDeposits, updateAllBalances }: UseCases): Express => {
  const app = express()

  app.get('/time-deposits', async (_req: Request, res: Response<TimeDepositResponse[]>) => {
    const details = await listTimeDeposits.listAll()
    res.json(details.map(toResponse))
  })

  app.post('/time-deposits/balance-updates', async (_req: Request, res: Response) => {
    await updateAllBalances.updateAllBalances()
    res.status(204).end()
  })

  const contract = parse(readFileSync(OPENAPI_PATH, 'utf8'))
  app.get('/api-docs', (_req, res) => res.json(contract))
  app.use('/swagger-ui', swaggerUi.serve, swaggerUi.setup(contract))

  return app
}

const toResponse = ({ deposit, withdrawals }: TimeDepositDetails): TimeDepositResponse => ({
  id: deposit.id,
  planType: deposit.planType,
  balance: deposit.balance,
  days: deposit.days,
  withdrawals: withdrawals.map(({ id, amount, date }) => ({ id, amount, date })),
})
