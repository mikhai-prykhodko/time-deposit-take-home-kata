package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class UpdateAllBalancesService implements UpdateAllBalancesUseCase {

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    UpdateAllBalancesService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    /** One transaction: either every balance is updated or none is. */
    @Override
    @Transactional
    public void updateAllBalances() {
        List<TimeDeposit> deposits = repository.findAll();
        calculator.updateBalance(deposits);
        repository.updateBalances(deposits);
    }
}
