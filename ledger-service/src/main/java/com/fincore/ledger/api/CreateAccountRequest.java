package com.fincore.ledger.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating an account. DTOs are the public API contract —
 * they are NOT the JPA entity. This boundary prevents your DB schema from
 * leaking into your API. If the entity gains a column, the API doesn't
 * change unless you explicitly add it here.
 */
public record CreateAccountRequest(
        @NotBlank String accountRef,
        @NotBlank String accountType,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}
