package com.fincore.ledger.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record JournalLineRequest(
        @NotBlank String accountRef,
        @NotBlank String entryType,
        @NotNull @Positive BigDecimal amount
) {}
