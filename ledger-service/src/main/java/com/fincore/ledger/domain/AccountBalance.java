package com.fincore.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Read-optimized running balance for an account, rebuilt from
 * {@link LedgerEntry} rows. This is the only table the application ever
 * updates in place (as opposed to appending) — it is a projection, not
 * financial history. Uses optimistic locking since concurrent postings
 * to the same account are expected.
 */
@Entity
@Table(name = "account_balances")
public class AccountBalance {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    protected AccountBalance() {
        // JPA
    }

    public AccountBalance(UUID accountId) {
        this.accountId = accountId;
        this.balance = BigDecimal.ZERO;
        this.updatedAt = OffsetDateTime.now();
    }

    public void apply(EntryType entryType, BigDecimal amount, AccountType accountType) {
        boolean increases = normalBalanceSide(accountType) == entryType;
        this.balance = increases ? this.balance.add(amount) : this.balance.subtract(amount);
        this.updatedAt = OffsetDateTime.now();
    }

    private static EntryType normalBalanceSide(AccountType accountType) {
        return switch (accountType) {
            case ASSET, EXPENSE -> EntryType.DEBIT;
            case LIABILITY, EQUITY, REVENUE -> EntryType.CREDIT;
        };
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
