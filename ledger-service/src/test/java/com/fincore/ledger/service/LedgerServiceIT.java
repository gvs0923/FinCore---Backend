package com.fincore.ledger.service;

import com.fincore.ledger.TestcontainersConfiguration;
import com.fincore.ledger.domain.Account;
import com.fincore.ledger.domain.AccountBalance;
import com.fincore.ledger.domain.AccountType;
import com.fincore.ledger.repository.AccountBalanceRepository;
import com.fincore.ledger.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LedgerServiceIT {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountBalanceRepository accountBalanceRepository;

    private Account wallet;
    private Account receivable;

    @BeforeEach
    void setUp() {
        wallet = accountRepository.save(new Account("TEST:CUSTOMER_WALLET:" + System.nanoTime(), AccountType.LIABILITY, "USD"));
        receivable = accountRepository.save(new Account("TEST:MERCHANT_RECEIVABLE:" + System.nanoTime(), AccountType.ASSET, "USD"));

        // Account provisioning creates the zero balance row up front. Without
        // this, concurrent first-time postings would race on inserting the
        // same balance row (nothing exists yet to lock), which is the bug
        // concurrentPostingsToSameAccount_serializeCorrectly below guards against.
        accountBalanceRepository.save(new AccountBalance(wallet.getId()));
        accountBalanceRepository.save(new AccountBalance(receivable.getId()));
    }

    @Test
    void postingBalancedTransaction_updatesBothAccountBalances() {
        ledgerService.postTransaction(
                "TX-" + System.nanoTime(),
                "test payment",
                List.of(
                        JournalLine.debit(receivable.getAccountRef(), new BigDecimal("100.00")),
                        JournalLine.credit(wallet.getAccountRef(), new BigDecimal("100.00"))
                )
        );

        BigDecimal receivableBalance = accountBalanceRepository.findById(receivable.getId())
                .orElseThrow().getBalance();
        BigDecimal walletBalance = accountBalanceRepository.findById(wallet.getId())
                .orElseThrow().getBalance();

        assertThat(receivableBalance).isEqualByComparingTo("100.00");
        assertThat(walletBalance).isEqualByComparingTo("100.00");
    }

    @Test
    void postingUnbalancedTransaction_isRejected() {
        assertThrows(UnbalancedTransactionException.class, () -> ledgerService.postTransaction(
                "TX-" + System.nanoTime(),
                "unbalanced",
                List.of(
                        JournalLine.debit(receivable.getAccountRef(), new BigDecimal("100.00")),
                        JournalLine.credit(wallet.getAccountRef(), new BigDecimal("90.00"))
                )
        ));
    }

    @Test
    void postingSameReferenceTwice_isRejected() {
        String reference = "TX-" + System.nanoTime();
        List<JournalLine> lines = List.of(
                JournalLine.debit(receivable.getAccountRef(), new BigDecimal("50.00")),
                JournalLine.credit(wallet.getAccountRef(), new BigDecimal("50.00"))
        );

        ledgerService.postTransaction(reference, "first post", lines);

        assertThrows(IllegalStateException.class, () ->
                ledgerService.postTransaction(reference, "duplicate post", lines));
    }

    @Test
    void concurrentPostingsToSameAccount_serializeCorrectly() throws InterruptedException {
        int concurrentPostings = 10;
        BigDecimal amountPerPosting = new BigDecimal("10.00");
        ExecutorService executor = Executors.newFixedThreadPool(concurrentPostings);
        CountDownLatch latch = new CountDownLatch(concurrentPostings);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < concurrentPostings; i++) {
            int idx = i;
            executor.submit(() -> {
                try {
                    ledgerService.postTransaction(
                            "TX-CONCURRENT-" + idx + "-" + System.nanoTime(),
                            "concurrent test",
                            List.of(
                                    JournalLine.debit(receivable.getAccountRef(), amountPerPosting),
                                    JournalLine.credit(wallet.getAccountRef(), amountPerPosting)
                            )
                    );
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        BigDecimal expectedTotal = amountPerPosting.multiply(new BigDecimal(concurrentPostings));
        BigDecimal actualBalance = accountBalanceRepository.findById(wallet.getId())
                .orElseThrow().getBalance();

        assertThat(successCount.get()).isEqualTo(concurrentPostings);
        assertThat(actualBalance).isEqualByComparingTo(expectedTotal);
    }
}
