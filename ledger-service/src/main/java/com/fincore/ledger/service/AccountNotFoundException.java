package com.fincore.ledger.service;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountRef) {
        super("No account found for accountRef: " + accountRef);
    }
}
