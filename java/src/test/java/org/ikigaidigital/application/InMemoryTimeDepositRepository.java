package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.TimeDepositRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Fake port for fast use case tests: no Spring, no database. */
class InMemoryTimeDepositRepository implements TimeDepositRepository {

    private final Map<Integer, TimeDeposit> deposits = new TreeMap<>();
    private final Map<Integer, List<Withdrawal>> withdrawals = new TreeMap<>();

    InMemoryTimeDepositRepository add(TimeDeposit deposit, Withdrawal... depositWithdrawals) {
        deposits.put(deposit.getId(), deposit);
        withdrawals.put(deposit.getId(), List.of(depositWithdrawals));
        return this;
    }

    @Override
    public List<TimeDeposit> findAll() {
        // copies, like a real database: changing them does not change stored data until updateBalances
        return deposits.values().stream()
            .map(d -> new TimeDeposit(d.getId(), d.getPlanType(), d.getBalance(), d.getDays()))
            .toList();
    }

    @Override
    public List<TimeDepositDetails> findAllWithWithdrawals() {
        return findAll().stream()
            .map(d -> new TimeDepositDetails(d, withdrawals.getOrDefault(d.getId(), new ArrayList<>())))
            .toList();
    }

    @Override
    public void updateBalances(List<TimeDeposit> updated) {
        updated.forEach(u -> deposits.get(u.getId()).setBalance(u.getBalance()));
    }

    double balanceOf(int id) {
        return deposits.get(id).getBalance();
    }
}
