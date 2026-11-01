import { TimeDeposit } from '../../TimeDeposit'
import { ListTimeDepositsService } from '../../application/ListTimeDepositsService'
import { InMemoryTimeDepositRepository } from '../support/InMemoryTimeDepositRepository'

describe('ListTimeDepositsService', () => {
  test('returns every deposit with its withdrawals', async () => {
    const withdrawal = { id: 1, amount: 100, date: '2024-01-15' }
    const repository = new InMemoryTimeDepositRepository()
      .add(new TimeDeposit(2, 'premium', 5000, 100))
      .add(new TimeDeposit(1, 'basic', 1000, 45), withdrawal)

    const details = await new ListTimeDepositsService(repository).listAll()

    expect(details.map((d) => d.deposit.id)).toEqual([1, 2])
    expect(details[0].withdrawals).toEqual([withdrawal])
    expect(details[1].withdrawals).toEqual([])
  })

  test('returns nothing when there are no deposits', async () => {
    expect(await new ListTimeDepositsService(new InMemoryTimeDepositRepository()).listAll()).toEqual([])
  })
})
