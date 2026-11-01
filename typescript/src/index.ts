import { Pool } from 'pg'
import { MIGRATION_DIR, SEED_DIR, SqlMigrator } from './adapters/out/postgres/SqlMigrator'
import { createApplication } from './compositionRoot'
import { logger } from './logger'

/*
 * Server bootstrap. Assumption: the original index.ts was a demo script that did not compile (it called
 * new TimeDeposit with three arguments instead of four) and is not part of the shared API, so it is
 * replaced by the server start-up.
 */
const port = Number(process.env.PORT ?? 3000)
// Defaults match typescript/docker-compose.yml.
const databaseUrl = process.env.DATABASE_URL ?? 'postgres://timedeposit:timedeposit@localhost:5433/timedeposit'

export const start = async (url = databaseUrl): Promise<void> => {
  const pool = new Pool({ connectionString: url })
  // Schema plus demo data, like the Java app's Flyway locations.
  await new SqlMigrator(pool).migrate(MIGRATION_DIR, SEED_DIR)
  createApplication(pool).listen(port, () => {
    logger.info(`Time deposit API on http://localhost:${port} (Swagger UI: /swagger-ui)`)
  })
}

if (require.main === module) {
  start().catch((error) => {
    logger.error(error)
    process.exit(1)
  })
}
