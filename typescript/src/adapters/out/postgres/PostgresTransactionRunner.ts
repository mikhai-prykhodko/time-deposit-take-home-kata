import { Pool } from 'pg'
import { TimeDepositRepository } from '../../../application/ports/TimeDepositRepository'
import { TransactionRunner } from '../../../application/ports/TransactionRunner'
import { PostgresTimeDepositRepository } from './PostgresTimeDepositRepository'

export class PostgresTransactionRunner implements TransactionRunner {
  constructor(private readonly pool: Pool) {}

  async run<T>(work: (repository: TimeDepositRepository) => Promise<T>): Promise<T> {
    const client = await this.pool.connect()
    try {
      await client.query('BEGIN')
      const result = await work(new PostgresTimeDepositRepository(client))
      await client.query('COMMIT')
      return result
    } catch (error) {
      await client.query('ROLLBACK')
      throw error
    } finally {
      client.release()
    }
  }
}
