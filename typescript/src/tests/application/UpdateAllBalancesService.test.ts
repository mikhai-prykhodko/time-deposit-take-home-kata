import { TimeDeposit } from '../../TimeDeposit'
import { TimeDepositCalculator } from '../../TimeDepositCalculator'
import { UpdateAllBalancesService } from '../../application/UpdateAllBalancesService'
import { InMemoryTimeDepositRepository } from '../support/InMemoryTimeDepositRepository'

const serviceFor = (repository: InMemoryTimeDepositRepository) =>
  new UpdateAllBalancesService(repository, new TimeDepositCalculator())

describe('UpdateAllBalancesService', () => {
  test('stores the balance the calculator produces for every deposit', async () => {
    const repository = new InMemoryTimeDepositRepository()
      .add(new TimeDeposit(1, 'basic', 1200, 31))
      .add(new TimeDeposit(2, 'student', 1200, 366))
      .add(new TimeDeposit(3, 'premium', 1200, 46))
      .add(new TimeDeposit(4, 'basic', 1200, 30))

    await serviceFor(repository).updateAllBalances()

    // basic +1.00, student past a year +0, premium +5.00, basic inside the first 30 days +0
    expect([1, 2, 3, 4].map((id) => repository.balanceOf(id))).toEqual([1201, 1200, 1205, 1200])
  })

  test('each call applies another month of interest', async () => {
    const repository = new InMemoryTimeDepositRepository().add(new TimeDeposit(1, 'basic', 1200, 31))
    const service = serviceFor(repository)

    await service.updateAllBalances()
    await service.updateAllBalances()

    expect(repository.balanceOf(1)).toBe(1202)
  })

  test('does nothing when there are no deposits', async () => {
    const repository = new InMemoryTimeDepositRepository()

    await serviceFor(repository).updateAllBalances()

    expect(await repository.findAll()).toEqual([])
  })
})
