import { PostgreSqlContainer, StartedPostgreSqlContainer } from '@testcontainers/postgresql'
import { Pool } from 'pg'
import { MIGRATION_DIR, SEED_DIR, SqlMigrator } from '../../../../adapters/out/postgres/SqlMigrator'

jest.setTimeout(120_000)

let container: StartedPostgreSqlContainer
let pool: Pool

beforeAll(async () => {
  container = await new PostgreSqlContainer('postgres:16-alpine').start()
  pool = new Pool({ connectionString: container.getConnectionUri() })
})

afterAll(async () => {
  await pool?.end()
  await container?.stop()
})

test('applies schema then seed once, records them, and a second run applies nothing', async () => {
  const migrator = new SqlMigrator(pool)

  expect(await migrator.migrate(MIGRATION_DIR, SEED_DIR)).toEqual([
    'V1 create time deposits and withdrawals',
    'V2 seed demo data',
  ])
  expect(await migrator.migrate(MIGRATION_DIR, SEED_DIR)).toEqual([])

  const history = await pool.query('SELECT version FROM schema_history ORDER BY version')
  expect(history.rows).toEqual([{ version: 1 }, { version: 2 }])
  const deposits = await pool.query('SELECT count(*)::int AS n FROM time_deposits')
  expect(deposits.rows[0].n).toBe(6)
})
