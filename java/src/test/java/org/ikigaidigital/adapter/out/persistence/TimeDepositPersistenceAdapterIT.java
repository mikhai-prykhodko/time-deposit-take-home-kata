package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositDetails;
import org.ikigaidigital.application.Withdrawal;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Schema only: the demo seed in db/seed is not loaded, so each test controls its own data.
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration")
@Import(TimeDepositPersistenceAdapter.class)
class TimeDepositPersistenceAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TimeDepositRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void insertTestData() {
        jdbc.update("DELETE FROM withdrawals");
        jdbc.update("DELETE FROM time_deposits");
        jdbc.update("INSERT INTO time_deposits (id, plan_type, days, balance) VALUES (2, 'student', 100, 2000.00)");
        jdbc.update("INSERT INTO time_deposits (id, plan_type, days, balance) VALUES (1, 'basic', 45, 1234567.00)");
        jdbc.update("INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES (11, 1, 50.25, '2024-02-20')");
        jdbc.update("INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES (10, 1, 100.00, '2024-01-15')");
    }

    @Test
    void findAllReturnsDomainDepositsOrderedById() {
        List<TimeDeposit> deposits = repository.findAll();

        assertThat(deposits).extracting(TimeDeposit::getId).containsExactly(1, 2);
        assertThat(deposits.get(0).getPlanType()).isEqualTo("basic");
        assertThat(deposits.get(0).getBalance()).isEqualTo(1234567.00);
        assertThat(deposits.get(0).getDays()).isEqualTo(45);
    }

    @Test
    void findAllWithWithdrawalsAttachesWithdrawalsOrderedById() {
        List<TimeDepositDetails> details = repository.findAllWithWithdrawals();

        assertThat(details).extracting(d -> d.deposit().getId()).containsExactly(1, 2);
        assertThat(details.get(0).withdrawals()).containsExactly(
            new Withdrawal(10, new BigDecimal("100.00"), LocalDate.of(2024, 1, 15)),
            new Withdrawal(11, new BigDecimal("50.25"), LocalDate.of(2024, 2, 20)));
        assertThat(details.get(1).withdrawals()).isEmpty();
    }

    @Test
    void updateBalancesStoresTheNewBalanceWithoutTouchingOtherColumns() {
        TimeDeposit updated = new TimeDeposit(1, "basic", 1235595.81, 45);

        repository.updateBalances(List.of(updated));
        flushAndClear();

        assertThat(jdbc.queryForObject("SELECT balance FROM time_deposits WHERE id = 1", BigDecimal.class))
            .isEqualByComparingTo("1235595.81");
        assertThat(jdbc.queryForObject("SELECT balance FROM time_deposits WHERE id = 2", BigDecimal.class))
            .isEqualByComparingTo("2000.00");
        assertThat(jdbc.queryForObject("SELECT plan_type || days FROM time_deposits WHERE id = 1", String.class))
            .isEqualTo("basic45");
    }

    @Test
    void balanceSurvivesTheDoubleToDecimalRoundTripWithoutDrift() {
        repository.updateBalances(List.of(new TimeDeposit(2, "student", 2002.5, 100)));
        flushAndClear();

        assertThat(repository.findAll()).extracting(TimeDeposit::getBalance).containsExactly(1234567.00, 2002.5);
    }

    /** Writes pending changes to Postgres and empties Hibernate's cache, so reads really hit the database. */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
