package com.fincore.ledger.api;

import com.fincore.ledger.domain.EntryType;
import com.fincore.ledger.domain.LedgerTransaction;
import com.fincore.ledger.service.JournalLine;
import com.fincore.ledger.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for posting ledger transactions. This is the write path —
 * the core operation of the entire platform. Every financial event
 * funnels through here.
 */
@RestController
@RequestMapping("/v1/transactions")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse postTransaction(@Valid @RequestBody PostTransactionRequest request) {
        List<JournalLine> lines = request.lines().stream()
                .map(line -> new JournalLine(
                        line.accountRef(),
                        EntryType.valueOf(line.entryType().toUpperCase()),
                        line.amount()
                ))
                .toList();

        LedgerTransaction tx = ledgerService.postTransaction(
                request.reference(),
                request.description(),
                lines
        );

        return TransactionResponse.from(tx);
    }
}
