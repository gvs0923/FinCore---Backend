package com.fincore.ledger.service;

import com.fincore.ledger.domain.Account;
import com.fincore.ledger.domain.AccountBalance;
import com.fincore.ledger.domain.AccountType;
import com.fincore.ledger.repository.AccountBalanceRepository;
import com.fincore.ledger.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Handles account lifecycle. The critical invariant is ADR-001: creating
 * an account MUST also create a zero-balance row in account_balances, in
 * the same transaction. Without that row, the first concurrent postings
 * would race on inserting it (nothing exists yet to lock with
 * SELECT FOR UPDATE).
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository balanceRepository;

    public AccountService(AccountRepository accountRepository,
                          AccountBalanceRepository balanceRepository) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
    }

    /**
     * Creates a new account and provisions its zero-balance row atomically.
     */
    @Transactional
    public Account createAccount(String accountRef, AccountType accountType, String currency) {
        accountRepository.findByAccountRef(accountRef).ifPresent(existing -> {
            throw new IllegalStateException("Account already exists: " + accountRef);
        });

        Account account = accountRepository.save(new Account(accountRef, accountType, currency));
        balanceRepository.save(new AccountBalance(account.getId()));
        return account;
    }

    /**
     * Returns the current balance for an account, looked up by its business
     * reference. This uses findById (no lock) — it is a read path, safe for
     * dashboards and API consumers.
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountRef) {
        Account account = accountRepository.findByAccountRef(accountRef)
                .orElseThrow(() -> new AccountNotFoundException(accountRef));
        return balanceRepository.findById(account.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Balance row missing for account " + accountRef + " — ADR-001 violated"))
                .getBalance();
    }

    @Transactional(readOnly = true)
    public Account getAccount(String accountRef) {
        return accountRepository.findByAccountRef(accountRef)
                .orElseThrow(() -> new AccountNotFoundException(accountRef));
    }

    @Transactional(readOnly = true)
    public Account getAccountById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id.toString()));
    }
}
