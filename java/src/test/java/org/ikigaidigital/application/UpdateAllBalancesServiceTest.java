package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateAllBalancesServiceTest {

    @Test
    void storesTheBalanceTheCalculatorProducesForEveryDeposit() {
        var repository = new InMemoryTimeDepositRepository()
            .add(new TimeDeposit(1, "basic", 1200.00, 31))
            .add(new TimeDeposit(2, "student", 1200.00, 366))
            .add(new TimeDeposit(3, "premium", 1200.00, 46))
            .add(new TimeDeposit(4, "basic", 1200.00, 30));

        new UpdateAllBalancesService(repository, new TimeDepositCalculator()).updateAllBalances();

        // basic +1.00, student past a year +0, premium +5.00, basic inside the first 30 days +0
        assertThat(repository.balanceOf(1)).isEqualTo(1201.00);
        assertThat(repository.balanceOf(2)).isEqualTo(1200.00);
        assertThat(repository.balanceOf(3)).isEqualTo(1205.00);
        assertThat(repository.balanceOf(4)).isEqualTo(1200.00);
    }

    @Test
    void eachCallAppliesAnotherMonthOfInterest() {
        var repository = new InMemoryTimeDepositRepository().add(new TimeDeposit(1, "basic", 1200.00, 31));
        var service = new UpdateAllBalancesService(repository, new TimeDepositCalculator());

        service.updateAllBalances();
        service.updateAllBalances();

        assertThat(repository.balanceOf(1)).isEqualTo(1202.00);
    }

    @Test
    void doesNothingWhenThereAreNoDeposits() {
        var repository = new InMemoryTimeDepositRepository();

        new UpdateAllBalancesService(repository, new TimeDepositCalculator()).updateAllBalances();

        assertThat(repository.findAll()).isEmpty();
    }
}
