import { PostgreSqlContainer } from '@testcontainers/postgresql'
import { start } from '../index'

/**
 * Runs the real application against a throwaway Postgres (Docker required), with the demo data loaded:
 * yarn dev:containers. The container is removed when the process stops. (Java: mvn spring-boot:test-run.)
 */
const main = async (): Promise<void> => {
  const container = await new PostgreSqlContainer('postgres:16-alpine').start()
  console.log(`Postgres (Testcontainers) at ${container.getConnectionUri()}`)
  await start(container.getConnectionUri())
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
