package com.fincore.ledger.service;

import java.math.BigDecimal;

/**
 * Thrown when a proposed set of journal lines would violate the
 * fundamental ledger invariant: total debits must equal total credits.
 */
public class UnbalancedTransactionException extends RuntimeException {

    public UnbalancedTransactionException(BigDecimal totalDebits, BigDecimal totalCredits) {
        super("Unbalanced transaction: total debits " + totalDebits
                + " != total credits " + totalCredits);
    }
}
