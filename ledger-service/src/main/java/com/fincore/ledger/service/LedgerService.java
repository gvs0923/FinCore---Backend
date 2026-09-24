package com.fincore.ledger.service;

import com.fincore.ledger.domain.Account;
import com.fincore.ledger.domain.AccountBalance;
import com.fincore.ledger.domain.EntryType;
import com.fincore.ledger.domain.LedgerEntry;
import com.fincore.ledger.domain.LedgerTransaction;
import com.fincore.ledger.repository.AccountBalanceRepository;
import com.fincore.ledger.repository.AccountRepository;
import com.fincore.ledger.repository.LedgerEntryRepository;
import com.fincore.ledger.repository.LedgerTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * The ledger is FinCore's source of financial truth. Every financial
 * event is posted here as a balanced set of journal entries; nothing
 * about account state is ever mutated destructively — only appended to.
 */
@Service
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final AccountBalanceRepository balanceRepository;

    public LedgerService(AccountRepository accountRepository,
                          LedgerTransactionRepository transactionRepository,
                          LedgerEntryRepository entryRepository,
                          AccountBalanceRepository balanceRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.balanceRepository = balanceRepository;
    }

    /**
     * Posts a balanced transaction: validates that debits equal credits,
     * writes the immutable journal entries, and updates the read-optimized
     * balance projection for each affected account — all atomically.
     *
     * @param reference   a unique business reference (e.g. payment id).
     *                     Re-posting the same reference is rejected, which
     *                     is what makes ledger posting safe to retry.
     * @param description human-readable description of the transaction.
     * @param lines       the journal lines. Must contain at least one debit
     *                     and one credit, and total debits must equal total
     *                     credits.
     */
    @Transactional
    public LedgerTransaction postTransaction(String reference, String description, List<JournalLine> lines) {
        assertBalanced(lines);

        transactionRepository.findByReference(reference).ifPresent(existing -> {
            throw new IllegalStateException("Ledger transaction already posted for reference: " + reference);
        });

        LedgerTransaction transaction = transactionRepository.save(new LedgerTransaction(reference, description));

        // Lock accounts in a deterministic order (by accountRef) to avoid
        // deadlocks when concurrent transactions touch overlapping accounts.
        List<JournalLine> orderedLines = lines.stream()
                .sorted(Comparator.comparing(JournalLine::accountRef))
                .toList();

        for (JournalLine line : orderedLines) {
            Account account = accountRepository.findByAccountRef(line.accountRef())
                    .orElseThrow(() -> new AccountNotFoundException(line.accountRef()));

            entryRepository.save(new LedgerEntry(transaction, account, line.entryType(), line.amount(), account.getCurrency()));

            // ADR-001: balance row MUST exist — it was created when the account
            // was created. If it's missing, that's a data integrity violation,
            // not something we paper over with lazy creation.
            AccountBalance balance = balanceRepository.findByAccountId(account.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Balance row missing for account " + account.getAccountRef()
                                    + " — ADR-001 violated. Was the account created via AccountService?"));
            balance.apply(line.entryType(), line.amount(), account.getAccountType());
            balanceRepository.save(balance);
        }

        return transaction;
    }

    private static void assertBalanced(List<JournalLine> lines) {
        if (lines.size() < 2) {
            throw new IllegalArgumentException("A transaction requires at least one debit and one credit line");
        }
        BigDecimal totalDebits = sum(lines, EntryType.DEBIT);
        BigDecimal totalCredits = sum(lines, EntryType.CREDIT);
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new UnbalancedTransactionException(totalDebits, totalCredits);
        }
    }

    private static BigDecimal sum(List<JournalLine> lines, EntryType type) {
        return lines.stream()
                .filter(line -> line.entryType() == type)
                .map(JournalLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
