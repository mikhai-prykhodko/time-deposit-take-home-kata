import { TimeDepositDetails } from './TimeDepositDetails'
import { ListTimeDepositsUseCase } from './ports/ListTimeDepositsUseCase'
import { TimeDepositRepository } from './ports/TimeDepositRepository'

export class ListTimeDepositsService implements ListTimeDepositsUseCase {
  constructor(private readonly repository: TimeDepositRepository) {}

  listAll(): Promise<TimeDepositDetails[]> {
    return this.repository.findAllWithWithdrawals()
  }
}
