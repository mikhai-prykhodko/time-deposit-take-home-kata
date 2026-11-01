package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/**
 * A time deposit together with its withdrawals.
 *
 * Assumption: the shared TimeDeposit class has no withdrawals and must not change, so the
 * withdrawals travel alongside it in this read model instead of inside it.
 *
 * Assumption: withdrawals are informational. No interest rule uses them, and the stored balance is
 * taken as already net of them, so the calculator never reads them.
 */
public record TimeDepositDetails(TimeDeposit deposit, List<Withdrawal> withdrawals) {
}
