package com.adam.banking.service;

import com.adam.banking.entity.Account;
import com.adam.banking.entity.Transaction;
import com.adam.banking.exception.FraudDetectedException;
import com.adam.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private Account alice;

    @BeforeEach
    void setUp() {
        alice = new Account("Alice Dupont", "alice@mail.com");
        alice.setId(1L);
        alice.setBalance(new BigDecimal("1000.00"));
    }

    // ===================== RULE 1: Large percentage of balance =====================

    @Test
    @DisplayName("Transaction > 50% of balance should be flagged as fraud")
    void largePercentage_shouldThrow() {
        // 600 > 50% of 1000 → fraud
        assertThrows(FraudDetectedException.class, () ->
            fraudDetectionService.checkForFraud(alice, new BigDecimal("600.00"))
        );
    }

    @Test
    @DisplayName("Transaction <= 50% of balance should pass")
    void normalPercentage_shouldPass() {
        when(transactionRepository.countTransactionsSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(transactionRepository.findRecentDeposits(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() ->
            fraudDetectionService.checkForFraud(alice, new BigDecimal("400.00"))
        );
    }

    // ===================== RULE 2: Transaction velocity =====================

    @Test
    @DisplayName("More than 5 transactions per minute should be flagged")
    void highVelocity_shouldThrow() {
        // Large balance so rule 1 doesn't trigger
        alice.setBalance(new BigDecimal("100000.00"));
        when(transactionRepository.countTransactionsSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(6L);

        assertThrows(FraudDetectedException.class, () ->
            fraudDetectionService.checkForFraud(alice, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("5 or fewer transactions per minute should pass velocity check")
    void normalVelocity_shouldPass() {
        alice.setBalance(new BigDecimal("100000.00"));
        when(transactionRepository.countTransactionsSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(4L);
        when(transactionRepository.findRecentDeposits(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() ->
            fraudDetectionService.checkForFraud(alice, new BigDecimal("100.00"))
        );
    }

    // ===================== RULE 3: Deposit then withdraw pattern =====================

    @Test
    @DisplayName("Withdrawal after large deposit within 5 minutes should be flagged")
    void depositThenWithdraw_shouldThrow() {
        alice.setBalance(new BigDecimal("100000.00"));
        when(transactionRepository.countTransactionsSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);

        // Simulate a recent large deposit of 1500€
        Transaction recentDeposit = Transaction.deposit(alice, new BigDecimal("1500.00"));
        when(transactionRepository.findRecentDeposits(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(recentDeposit));

        assertThrows(FraudDetectedException.class, () ->
            fraudDetectionService.checkForFraud(alice, new BigDecimal("100.00"))
        );
    }
}
