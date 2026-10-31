package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/**
 * A time deposit together with its withdrawals.
 *
 * Assumption: the shared TimeDeposit class has no withdrawals and must not change, so the
 * withdrawals travel alongside it in this read model instead of inside it.
 */
public record TimeDepositDetails(TimeDeposit deposit, List<Withdrawal> withdrawals) {
}
