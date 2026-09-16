package com.fincore.ledger.repository;

import com.fincore.ledger.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtAsc(UUID accountId);
}
