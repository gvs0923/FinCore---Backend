package com.fincore.ledger.service;

import com.fincore.ledger.domain.Account;
import com.fincore.ledger.domain.AccountBalance;
import com.fincore.ledger.domain.LedgerEntry;
import com.fincore.ledger.repository.AccountBalanceRepository;
import com.fincore.ledger.repository.AccountRepository;
import com.fincore.ledger.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Rebuilds the account_balances projection from the source-of-truth
 * ledger_entries. This proves that account_balances is derived data,
 * not a second source of truth — if it drifts (bug, manual fix,
 * restore from backup), it can be recomputed.
 *
 * <p>The same idea appears in event sourcing as "replaying the event
 * log to rebuild a projection." You'll see this pattern again in
 * Phase 3 when Kafka consumers maintain their own projections.
 */
@Service
public class BalanceProjectionService {

    private static final Logger log = LoggerFactory.getLogger(BalanceProjectionService.class);

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;
    private final AccountBalanceRepository balanceRepository;

    public BalanceProjectionService(AccountRepository accountRepository,
                                     LedgerEntryRepository entryRepository,
                                     AccountBalanceRepository balanceRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.balanceRepository = balanceRepository;
    }

    /**
     * Recomputes the balance for a single account by replaying all its
     * ledger entries from zero.
     *
     * @return the recomputed balance
     */
    @Transactional
    public BigDecimal rebuildForAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        AccountBalance balance = balanceRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException(
                        "Balance row missing for account " + accountId + " — ADR-001 violated"));

        List<LedgerEntry> entries = entryRepository.findByAccountIdOrderByCreatedAtAsc(accountId);

        // Reset to zero and replay every entry
        BigDecimal recomputed = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            boolean increases = normalBalanceSide(account.getAccountType()) == entry.getEntryType();
            recomputed = increases ? recomputed.add(entry.getAmount()) : recomputed.subtract(entry.getAmount());
        }

        BigDecimal before = balance.getBalance();
        balance.overwriteBalance(recomputed);
        balanceRepository.save(balance);

        if (before.compareTo(recomputed) != 0) {
            log.warn("Drift detected for account {}: was {} → rebuilt to {}",
                    accountId, before, recomputed);
        }

        return recomputed;
    }

    /**
     * Rebuilds balances for ALL accounts. Use with care — this is an
     * operational recovery tool, not a normal code path.
     *
     * @return the number of accounts rebuilt
     */
    @Transactional
    public int rebuildAll() {
        List<Account> accounts = accountRepository.findAll();
        int count = 0;
        for (Account account : accounts) {
            rebuildForAccount(account.getId());
            count++;
        }
        log.info("Rebuilt balance projection for {} accounts", count);
        return count;
    }

    private static com.fincore.ledger.domain.EntryType normalBalanceSide(
            com.fincore.ledger.domain.AccountType accountType) {
        return switch (accountType) {
            case ASSET, EXPENSE -> com.fincore.ledger.domain.EntryType.DEBIT;
            case LIABILITY, EQUITY, REVENUE -> com.fincore.ledger.domain.EntryType.CREDIT;
        };
    }
}
