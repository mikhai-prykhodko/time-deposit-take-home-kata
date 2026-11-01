import { Express } from 'express'
import { Pool } from 'pg'
import { TimeDepositCalculator } from './TimeDepositCalculator'
import { createApp } from './adapters/in/http/createApp'
import { PostgresTimeDepositRepository } from './adapters/out/postgres/PostgresTimeDepositRepository'
import { PostgresTransactionRunner } from './adapters/out/postgres/PostgresTransactionRunner'
import { ListTimeDepositsService } from './application/ListTimeDepositsService'
import { UpdateAllBalancesService } from './application/UpdateAllBalancesService'

/** Wires the adapters into the use cases (the Java solution lets Spring do this). */
export const createApplication = (pool: Pool): Express =>
  createApp({
    listTimeDeposits: new ListTimeDepositsService(new PostgresTimeDepositRepository(pool)),
    updateAllBalances: new UpdateAllBalancesService(new PostgresTransactionRunner(pool), new TimeDepositCalculator()),
  })
