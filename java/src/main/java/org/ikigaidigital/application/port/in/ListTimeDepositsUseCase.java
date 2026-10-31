package org.ikigaidigital.application.port.in;

import org.ikigaidigital.application.TimeDepositDetails;

import java.util.List;

public interface ListTimeDepositsUseCase {

    List<TimeDepositDetails> listAll();
}
