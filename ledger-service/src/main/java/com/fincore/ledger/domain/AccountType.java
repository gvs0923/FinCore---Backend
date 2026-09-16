package com.fincore.ledger.domain;

/**
 * Standard double-entry account classifications. Determines which side
 * (debit or credit) increases the account's normal balance, which matters
 * once we start rendering human-readable balances rather than raw ledger
 * entries.
 */
public enum AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE
}
