package org.ikigaidigital.application;

import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ListTimeDepositsService implements ListTimeDepositsUseCase {

    private final TimeDepositRepository repository;

    ListTimeDepositsService(TimeDepositRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TimeDepositDetails> listAll() {
        return repository.findAllWithWithdrawals();
    }
}
