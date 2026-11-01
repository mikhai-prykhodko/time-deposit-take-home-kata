import { readdirSync, readFileSync } from 'fs'
import { join } from 'path'
import { Pool } from 'pg'

/**
 * Minimal Flyway-style runner for the same SQL files the Java solution uses.
 * Files are named V<version>__<description>.sql, applied in version order, each in its own
 * transaction, and recorded in schema_history so a second run applies nothing.
 * (node-pg-migrate was the first choice; its current versions are ESM-only, which this CommonJS
 * project cannot load cleanly, and the job is small enough to keep in one tested class.)
 */
export class SqlMigrator {
  constructor(private readonly pool: Pool) {}

  async migrate(...directories: string[]): Promise<string[]> {
    await this.pool.query(`CREATE TABLE IF NOT EXISTS schema_history (
      version     INTEGER PRIMARY KEY,
      description TEXT        NOT NULL,
      applied_at  TIMESTAMPTZ NOT NULL DEFAULT now()
    )`)
    const { rows } = await this.pool.query<{ version: number }>('SELECT version FROM schema_history')
    const applied = new Set(rows.map((row) => row.version))

    const pending = directories
      .flatMap((directory) => readdirSync(directory).map((file) => ({ directory, file })))
      .map(({ directory, file }) => ({ path: join(directory, file), ...parseName(file) }))
      .filter((migration) => !applied.has(migration.version))
      .sort((a, b) => a.version - b.version)

    for (const migration of pending) {
      const client = await this.pool.connect()
      try {
        await client.query('BEGIN')
        await client.query(readFileSync(migration.path, 'utf8'))
        await client.query('INSERT INTO schema_history (version, description) VALUES ($1, $2)', [
          migration.version,
          migration.description,
        ])
        await client.query('COMMIT')
      } catch (error) {
        await client.query('ROLLBACK')
        throw error
      } finally {
        client.release()
      }
    }
    return pending.map((migration) => `V${migration.version} ${migration.description}`)
  }
}

const parseName = (file: string): { version: number; description: string } => {
  const match = /^V(\d+)__(.+)\.sql$/.exec(file)
  if (!match) throw new Error(`Not a migration file name: ${file}`)
  return { version: Number(match[1]), description: match[2].replace(/_/g, ' ') }
}

export const MIGRATION_DIR = join(__dirname, '../../../../db/migration')
export const SEED_DIR = join(__dirname, '../../../../db/seed')
