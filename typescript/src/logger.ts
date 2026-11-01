import { ecsFormat } from '@elastic/ecs-pino-format'
import pino, { DestinationStream, Logger } from 'pino'

/**
 * Elastic Common Schema JSON on stdout, so ELK (Filebeat/Logstash) can ingest it without parsing rules.
 * The destination can be replaced, which the tests use to capture log lines.
 */
export const createLogger = (destination?: DestinationStream): Logger =>
  pino({ ...ecsFormat({ serviceName: 'time-deposit' }) }, destination)

export const logger = createLogger()
