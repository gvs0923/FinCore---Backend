package com.fincore.ledger.api;

import com.fincore.ledger.domain.LedgerTransaction;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String reference,
        String description,
        OffsetDateTime createdAt
) {
    public static TransactionResponse from(LedgerTransaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getReference(),
                tx.getDescription(),
                tx.getCreatedAt()
        );
    }
}
