package com.fincore.ledger.service;

import com.fincore.ledger.domain.EntryType;

import java.math.BigDecimal;

/**
 * One requested debit or credit line, referencing an account by its
 * business key (accountRef) rather than internal id, so callers don't
 * need to look up accounts themselves.
 */
public record JournalLine(String accountRef, EntryType entryType, BigDecimal amount) {

    public JournalLine {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Journal line amount must be positive: " + amount);
        }
    }

    public static JournalLine debit(String accountRef, BigDecimal amount) {
        return new JournalLine(accountRef, EntryType.DEBIT, amount);
    }

    public static JournalLine credit(String accountRef, BigDecimal amount) {
        return new JournalLine(accountRef, EntryType.CREDIT, amount);
    }
}
