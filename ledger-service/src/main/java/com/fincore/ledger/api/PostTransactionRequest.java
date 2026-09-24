package com.fincore.ledger.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for posting a ledger transaction. The caller provides a
 * unique reference (for idempotency) and the journal lines.
 */
public record PostTransactionRequest(
        @NotBlank String reference,
        String description,
        @NotEmpty @Valid List<JournalLineRequest> lines
) {}
