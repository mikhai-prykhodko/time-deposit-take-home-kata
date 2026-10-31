package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositDetails;
import org.ikigaidigital.application.Withdrawal;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional
class TimeDepositPersistenceAdapter implements TimeDepositRepository {

    private final TimeDepositJpaRepository jpaRepository;

    TimeDepositPersistenceAdapter(TimeDepositJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeDeposit> findAll() {
        return jpaRepository.findAllByOrderById().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeDepositDetails> findAllWithWithdrawals() {
        return jpaRepository.findAllWithWithdrawals().stream()
            .map(entity -> new TimeDepositDetails(toDomain(entity), toWithdrawals(entity)))
            .toList();
    }

    @Override
    public void updateBalances(List<TimeDeposit> deposits) {
        Map<Integer, TimeDeposit> byId = deposits.stream()
            .collect(Collectors.toMap(TimeDeposit::getId, Function.identity()));
        jpaRepository.findAllById(byId.keySet())
            .forEach(entity -> entity.setBalance(toDecimal(byId.get(entity.getId()).getBalance())));
    }

    private TimeDeposit toDomain(TimeDepositEntity entity) {
        return new TimeDeposit(entity.getId(), entity.getPlanType(), entity.getBalance().doubleValue(), entity.getDays());
    }

    private List<Withdrawal> toWithdrawals(TimeDepositEntity entity) {
        return entity.getWithdrawals().stream()
            .sorted(Comparator.comparing(WithdrawalEntity::getId))
            .map(w -> new Withdrawal(w.getId(), w.getAmount(), w.getDate()))
            .toList();
    }

    /**
     * The calculator works on Double, the database on NUMERIC(19,2). valueOf uses the shortest decimal
     * that identifies the double (1235595.81, not 1235595.8100000000), and the scale matches the column.
     */
    private BigDecimal toDecimal(Double balance) {
        return BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP);
    }
}
