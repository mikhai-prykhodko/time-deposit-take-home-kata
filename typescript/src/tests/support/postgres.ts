import { PostgreSqlContainer, StartedPostgreSqlContainer } from '@testcontainers/postgresql'
import { Pool } from 'pg'
import { MIGRATION_DIR, SqlMigrator } from '../../adapters/out/postgres/SqlMigrator'

export interface TestDatabase {
  container: StartedPostgreSqlContainer
  pool: Pool
}

/** A real Postgres 16 in Docker with the schema only (no demo seed), so each test controls its data. */
export const startTestDatabase = async (): Promise<TestDatabase> => {
  const container = await new PostgreSqlContainer('postgres:16-alpine').start()
  const pool = new Pool({ connectionString: container.getConnectionUri() })
  await new SqlMigrator(pool).migrate(MIGRATION_DIR)
  return { container, pool }
}

export const stopTestDatabase = async (db: TestDatabase | undefined): Promise<void> => {
  await db?.pool.end()
  await db?.container.stop()
}

export const resetData = async (pool: Pool): Promise<void> => {
  await pool.query('DELETE FROM withdrawals')
  await pool.query('DELETE FROM time_deposits')
}
