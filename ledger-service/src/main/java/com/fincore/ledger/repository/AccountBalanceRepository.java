package com.fincore.ledger.repository;

import com.fincore.ledger.domain.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {

    /**
     * Pessimistic lock so concurrent postings to the same account serialize
     * on the balance row rather than racing on a read-modify-write.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountBalance> findByAccountId(UUID accountId);
}
