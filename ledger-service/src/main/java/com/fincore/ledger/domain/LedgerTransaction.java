package com.fincore.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A single financial event (e.g. a payment capture). Immutable once
 * persisted: corrections happen via a new, reversing transaction, never
 * by updating this row or its entries.
 */
@Entity
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(length = 1000)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected LedgerTransaction() {
        // JPA
    }

    public LedgerTransaction(String reference, String description) {
        this.reference = reference;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
