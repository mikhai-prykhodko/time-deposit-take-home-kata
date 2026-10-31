package org.ikigaidigital.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositEntity, Integer> {

    List<TimeDepositEntity> findAllByOrderById();

    @Query("select distinct d from TimeDepositEntity d left join fetch d.withdrawals order by d.id")
    List<TimeDepositEntity> findAllWithWithdrawals();
}
