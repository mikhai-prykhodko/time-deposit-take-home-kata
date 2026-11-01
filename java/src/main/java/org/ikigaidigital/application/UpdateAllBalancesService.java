package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class UpdateAllBalancesService implements UpdateAllBalancesUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateAllBalancesService.class);

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    UpdateAllBalancesService(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    /**
     * One transaction: either every balance is updated or none is.
     *
     * Assumption: simultaneous calls are not coordinated (concurrency control is out of scope), so two
     * overlapping updates may each read the same balances and apply a single month between them.
     */
    @Override
    @Transactional
    public void updateAllBalances() {
        long startedAt = System.nanoTime();
        List<TimeDeposit> deposits = repository.findAll();
        calculator.updateBalance(deposits);
        repository.updateBalances(deposits);
        log.info("Balance update finished: {} time deposits updated in {} ms",
            deposits.size(), (System.nanoTime() - startedAt) / 1_000_000);
    }
}
