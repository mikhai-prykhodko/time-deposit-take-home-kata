package org.ikigaidigital.application.port.in;

public interface UpdateAllBalancesUseCase {

    /** Applies one month of interest to every time deposit and stores the new balances. */
    void updateAllBalances();
}
