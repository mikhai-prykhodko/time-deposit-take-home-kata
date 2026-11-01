import { TimeDepositDetails } from '../TimeDepositDetails'

export interface ListTimeDepositsUseCase {
  listAll(): Promise<TimeDepositDetails[]>
}
