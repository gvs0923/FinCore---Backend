package com.fincore.ledger.api;

import com.fincore.ledger.domain.Account;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountRef,
        String accountType,
        String currency,
        OffsetDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountRef(),
                account.getAccountType().name(),
                account.getCurrency(),
                account.getCreatedAt()
        );
    }
}
