package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.adapter.in.web.api.TimeDepositsApi;
import org.ikigaidigital.adapter.in.web.api.model.TimeDepositResponse;
import org.ikigaidigital.adapter.in.web.api.model.WithdrawalResponse;
import org.ikigaidigital.application.TimeDepositDetails;
import org.ikigaidigital.application.Withdrawal;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The routes and response shapes come from openapi.yaml through the generated TimeDepositsApi. */
@RestController
class TimeDepositController implements TimeDepositsApi {

    private final ListTimeDepositsUseCase listTimeDeposits;
    private final UpdateAllBalancesUseCase updateAllBalances;

    TimeDepositController(ListTimeDepositsUseCase listTimeDeposits, UpdateAllBalancesUseCase updateAllBalances) {
        this.listTimeDeposits = listTimeDeposits;
        this.updateAllBalances = updateAllBalances;
    }

    @Override
    public ResponseEntity<List<TimeDepositResponse>> listTimeDeposits() {
        return ResponseEntity.ok(listTimeDeposits.listAll().stream().map(this::toResponse).toList());
    }

    @Override
    public ResponseEntity<Void> updateAllBalances() {
        updateAllBalances.updateAllBalances();
        return ResponseEntity.noContent().build();
    }

    private TimeDepositResponse toResponse(TimeDepositDetails details) {
        var deposit = details.deposit();
        return new TimeDepositResponse()
            .id(deposit.getId())
            .planType(deposit.getPlanType())
            .balance(deposit.getBalance())
            .days(deposit.getDays())
            .withdrawals(details.withdrawals().stream().map(this::toResponse).toList());
    }

    private WithdrawalResponse toResponse(Withdrawal withdrawal) {
        return new WithdrawalResponse()
            .id(withdrawal.id())
            .amount(withdrawal.amount().doubleValue())
            .date(withdrawal.date());
    }
}
