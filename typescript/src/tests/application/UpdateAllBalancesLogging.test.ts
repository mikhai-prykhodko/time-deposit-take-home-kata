import { Writable } from 'stream'
import { TimeDeposit } from '../../TimeDeposit'
import { TimeDepositCalculator } from '../../TimeDepositCalculator'
import { UpdateAllBalancesService } from '../../application/UpdateAllBalancesService'
import { createLogger } from '../../logger'
import { InMemoryTimeDepositRepository } from '../support/InMemoryTimeDepositRepository'

test('logs one ECS JSON line with the number of updated deposits per run', async () => {
  const lines: string[] = []
  const capture = new Writable({
    write(chunk, _encoding, done) {
      lines.push(chunk.toString())
      done()
    },
  })
  const repository = new InMemoryTimeDepositRepository()
    .add(new TimeDeposit(1, 'basic', 1200, 31))
    .add(new TimeDeposit(2, 'premium', 1200, 46))

  await new UpdateAllBalancesService(repository, new TimeDepositCalculator(), createLogger(capture)).updateAllBalances()

  expect(lines).toHaveLength(1)
  const entry = JSON.parse(lines[0])
  expect(entry.message).toMatch(/^Balance update finished: 2 time deposits updated in \d+ ms$/)
  expect(entry['log.level']).toBe('info')
  expect(entry['service.name']).toBe('time-deposit')
  expect(entry['ecs.version']).toBeDefined()
})
