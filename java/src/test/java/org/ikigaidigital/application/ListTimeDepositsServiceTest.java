package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ListTimeDepositsServiceTest {

    @Test
    void returnsEveryDepositWithItsWithdrawals() {
        Withdrawal withdrawal = new Withdrawal(1, new BigDecimal("100.00"), LocalDate.of(2024, 1, 15));
        var repository = new InMemoryTimeDepositRepository()
            .add(new TimeDeposit(1, "basic", 1000.00, 45), withdrawal)
            .add(new TimeDeposit(2, "premium", 5000.00, 100));

        var details = new ListTimeDepositsService(repository).listAll();

        assertThat(details).extracting(d -> d.deposit().getId()).containsExactly(1, 2);
        assertThat(details.get(0).withdrawals()).containsExactly(withdrawal);
        assertThat(details.get(1).withdrawals()).isEmpty();
    }

    @Test
    void returnsNothingWhenThereAreNoDeposits() {
        assertThat(new ListTimeDepositsService(new InMemoryTimeDepositRepository()).listAll()).isEmpty();
    }
}
