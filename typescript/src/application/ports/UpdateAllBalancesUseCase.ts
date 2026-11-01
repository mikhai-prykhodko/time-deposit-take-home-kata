export interface UpdateAllBalancesUseCase {
  /** Applies one month of interest to every time deposit and stores the new balances. */
  updateAllBalances(): Promise<void>
}
