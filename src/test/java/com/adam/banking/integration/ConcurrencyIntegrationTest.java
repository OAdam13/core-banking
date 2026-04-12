package com.adam.banking.integration;

import com.adam.banking.entity.Account;
import com.adam.banking.repository.AccountRepository;
import com.adam.banking.service.FraudDetectionService;
import com.adam.banking.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INTEGRATION TEST: Proves pessimistic locking prevents the double-withdrawal race condition.
 *
 * Scenario:
 * - Account starts with 1000€
 * - 10 threads simultaneously attempt to withdraw 200€ each
 * - Total attempted: 10 × 200€ = 2000€
 * - Without locking: all 10 would succeed → balance goes negative
 * - With locking: only 5 succeed (5 × 200€ = 1000€), remaining 5 fail
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    // Mock fraud detection so it never blocks — we're testing LOCKING, not fraud rules
    @MockBean
    private FraudDetectionService fraudDetectionService;

    @Test
    @DisplayName("10 concurrent withdrawals of 200€ from 1000€ account — only 5 should succeed")
    void concurrentWithdrawals_shouldRespectBalance() throws InterruptedException {

        // ARRANGE: Create a test account with 1000€
        Account account = new Account("Test User", "test@concurrency.com");
        account.setBalance(new BigDecimal("1000.00"));
        account.setDailyLimit(new BigDecimal("10000.00")); // High limit so daily check doesn't interfere
        account = accountRepository.save(account);
        final Long accountId = account.getId();

        int threadCount = 10;
        BigDecimal withdrawalAmount = new BigDecimal("200.00");

        // CountDownLatch(1): all threads wait behind this gate
        // When we call startLatch.countDown(), the gate opens and all threads rush in at once
        CountDownLatch startLatch = new CountDownLatch(1);

        // CountDownLatch(10): main thread waits until all 10 threads are done
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // AtomicInteger: thread-safe counters (regular int would lose updates under concurrency)
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // ACT: Submit 10 threads — they all wait at the startLatch gate
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Block here until the gate opens
                    transactionService.withdraw(accountId, withdrawalAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // InsufficientFundsException or FraudDetectedException → expected for the last 5
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown(); // Signal: this thread is done
                }
            });
        }

        // Open the gate — all 10 threads start at the exact same moment
        startLatch.countDown();

        // Wait for all 10 threads to finish before asserting
        doneLatch.await();
        executor.shutdown();

        // ASSERT
        Account updatedAccount = accountRepository.findById(accountId).orElseThrow();

        assertEquals(
            0,
            updatedAccount.getBalance().compareTo(BigDecimal.ZERO),
            "Balance should be exactly 0€ after 5 successful withdrawals of 200€"
        );

        assertEquals(5, successCount.get(), "Exactly 5 withdrawals should succeed");
        assertEquals(5, failCount.get(), "Exactly 5 withdrawals should fail");

        System.out.println("=== CONCURRENCY TEST RESULTS ===");
        System.out.println("Successful withdrawals : " + successCount.get());
        System.out.println("Failed withdrawals     : " + failCount.get());
        System.out.println("Final balance          : " + updatedAccount.getBalance());
        System.out.println("================================");
    }
}
