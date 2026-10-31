package org.ikigaidigital.application.port.out;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositDetails;

import java.util.List;

public interface TimeDepositRepository {

    List<TimeDeposit> findAll();

    List<TimeDepositDetails> findAllWithWithdrawals();

    /** Stores the current balance of each given deposit; nothing else about a deposit changes. */
    void updateBalances(List<TimeDeposit> deposits);
}
