package org.ikigaidigital.application.port.out;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositDetails;

import java.util.List;

/** Deposits, and each deposit's withdrawals, are returned ordered by id so responses are stable. */
public interface TimeDepositRepository {

    List<TimeDeposit> findAll();

    List<TimeDepositDetails> findAllWithWithdrawals();

    /** Stores the current balance of each given deposit; nothing else about a deposit changes. */
    void updateBalances(List<TimeDeposit> deposits);
}
