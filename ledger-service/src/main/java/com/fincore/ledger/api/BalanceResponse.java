package com.fincore.ledger.api;

import java.math.BigDecimal;

public record BalanceResponse(
        String accountRef,
        BigDecimal balance,
        String currency
) {}
