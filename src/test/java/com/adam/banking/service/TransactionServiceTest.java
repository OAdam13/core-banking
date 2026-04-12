package com.adam.banking.service;

import com.adam.banking.entity.Account;
import com.adam.banking.entity.Transaction;
import com.adam.banking.enums.TransactionStatus;
import com.adam.banking.enums.TransactionType;
import com.adam.banking.exception.AccountNotFoundException;
import com.adam.banking.exception.DailyLimitExceededException;
import com.adam.banking.exception.InsufficientFundsException;
import com.adam.banking.repository.AccountRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @InjectMocks
    private TransactionService transactionService;

    private Account alice;

    @BeforeEach
    void setUp() {
        alice = new Account("Alice Dupont", "alice@mail.com");
        alice.setId(1L);
        alice.setBalance(new BigDecimal("1000.00"));
        alice.setDailyLimit(new BigDecimal("5000.00"));
    }

    // ===================== DEPOSIT TESTS =====================

    @Test
    @DisplayName("Deposit should increase balance and record transaction")
    void deposit_success() {
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(alice));
        when(accountRepository.save(any(Account.class))).thenReturn(alice);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = transactionService.deposit(1L, new BigDecimal("500.00"));

        assertEquals(new BigDecimal("1500.00"), alice.getBalance());
        assertEquals(TransactionType.DEPOSIT, tx.getType());
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());

        verify(accountRepository, times(1)).findByIdWithLock(1L);
        verify(accountRepository, times(1)).save(alice);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deposit to non-existent account should throw AccountNotFoundException")
    void deposit_accountNotFound() {
        when(accountRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () ->
            transactionService.deposit(999L, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("Deposit with negative amount should throw IllegalArgumentException")
    void deposit_negativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
            transactionService.deposit(1L, new BigDecimal("-100.00"))
        );
    }

    // ===================== WITHDRAWAL TESTS =====================

    @Test
    @DisplayName("Withdrawal with sufficient funds should succeed")
    void withdraw_success() {
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository.sumWithdrawalsSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(accountRepository.save(any(Account.class))).thenReturn(alice);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = transactionService.withdraw(1L, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("800.00"), alice.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, tx.getType());
        assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
    }

    @Test
    @DisplayName("Withdrawal exceeding balance should throw InsufficientFundsException")
    void withdraw_insufficientFunds() {
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository.sumWithdrawalsSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        assertThrows(InsufficientFundsException.class, () ->
            transactionService.withdraw(1L, new BigDecimal("1500.00"))
        );

        // Balance should NOT have changed
        assertEquals(new BigDecimal("1000.00"), alice.getBalance());
    }

    @Test
    @DisplayName("Withdrawal exceeding daily limit should throw DailyLimitExceededException")
    void withdraw_dailyLimitExceeded() {
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(alice));
        // Already withdrawn 4500€ today — only 500€ remaining
        when(transactionRepository.sumWithdrawalsSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("4500.00"));

        // Trying to withdraw 600€ more (4500 + 600 = 5100 > 5000)
        assertThrows(DailyLimitExceededException.class, () ->
            transactionService.withdraw(1L, new BigDecimal("600.00"))
        );
    }

    // ===================== TRANSFER TESTS =====================

    @Test
    @DisplayName("Transfer should debit source and credit target atomically")
    void transfer_success() {
        Account bob = new Account("Bob Martin", "bob@mail.com");
        bob.setId(2L);
        bob.setBalance(new BigDecimal("500.00"));
        bob.setDailyLimit(new BigDecimal("5000.00"));

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(alice));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(bob));
        when(transactionRepository.sumWithdrawalsSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction tx = transactionService.transfer(1L, 2L, new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), alice.getBalance());
        assertEquals(new BigDecimal("800.00"), bob.getBalance());
        assertEquals(TransactionType.TRANSFER_OUT, tx.getType());

        // Two transactions saved: TRANSFER_OUT + TRANSFER_IN
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Transfer to same account should throw IllegalArgumentException")
    void transfer_sameAccount() {
        assertThrows(IllegalArgumentException.class, () ->
            transactionService.transfer(1L, 1L, new BigDecimal("100.00"))
        );
    }
}
