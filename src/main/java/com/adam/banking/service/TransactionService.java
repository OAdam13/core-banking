package com.adam.banking.service;

import com.adam.banking.entity.Account;
import com.adam.banking.entity.Transaction;
import com.adam.banking.enums.TransactionType;
import com.adam.banking.exception.*;
import com.adam.banking.repository.AccountRepository;
import com.adam.banking.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudDetectionService fraudDetectionService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              FraudDetectionService fraudDetectionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fraudDetectionService = fraudDetectionService;
    }

    // =========================================================================
    // DEPOSIT
    // =========================================================================

    /**
     * Deposit money into an account.
     *
     * Even deposits use pessimistic locking because we're modifying the balance.
     * Without a lock, two concurrent deposits could cause a lost update.
     */
    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount) {
        validatePositiveAmount(amount);

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.credit(amount);
        accountRepository.save(account);

        Transaction tx = Transaction.deposit(account, amount);
        return transactionRepository.save(tx);
    }

    // =========================================================================
    // WITHDRAWAL — This is the most critical method in the entire project
    // =========================================================================

    /**
     * Withdraw money from an account.
     *
     * Order of operations:
     * 1. Validate amount
     * 2. Lock the account row (SELECT FOR UPDATE)
     * 3. Check daily limit
     * 4. Check fraud
     * 5. Check sufficient funds
     * 6. Debit
     * 7. Record transaction
     */
    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount) {
        validatePositiveAmount(amount);

        // STEP 1: Acquire pessimistic lock — blocks any other thread trying to touch this row
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // STEP 2: Sliding 24h daily limit check
        checkDailyLimit(account, amount);

        // STEP 3: Fraud detection — save a BLOCKED record before re-throwing
        try {
            fraudDetectionService.checkForFraud(account, amount);
        } catch (FraudDetectedException e) {
            Transaction blocked = Transaction.blocked(account, amount, TransactionType.WITHDRAWAL, e.getMessage());
            transactionRepository.save(blocked);
            throw e;
        }

        // STEP 4: Sufficient funds check (done AFTER lock, so the balance is guaranteed current)
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(amount, account.getBalance());
        }

        // STEP 5: Debit and save
        account.debit(amount);
        accountRepository.save(account);

        // STEP 6: Record the successful transaction
        Transaction tx = Transaction.withdrawal(account, amount);
        return transactionRepository.save(tx);
    }

    // =========================================================================
    // TRANSFER — Demonstrates deadlock prevention
    // =========================================================================

    /**
     * Transfer money between two accounts.
     *
     * CRITICAL: Accounts are always locked in ascending order of ID.
     * This eliminates the circular-wait condition that causes deadlocks.
     */
    @Transactional
    public Transaction transfer(Long sourceId, Long targetId, BigDecimal amount) {
        validatePositiveAmount(amount);

        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // DEADLOCK PREVENTION: always lock the lower ID first
        Long firstLockId = Math.min(sourceId, targetId);
        Long secondLockId = Math.max(sourceId, targetId);

        Account firstAccount = accountRepository.findByIdWithLock(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException(firstLockId));
        Account secondAccount = accountRepository.findByIdWithLock(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException(secondLockId));

        // Resolve which is source and which is target
        Account source = sourceId.equals(firstLockId) ? firstAccount : secondAccount;
        Account target = sourceId.equals(firstLockId) ? secondAccount : firstAccount;

        checkDailyLimit(source, amount);

        try {
            fraudDetectionService.checkForFraud(source, amount);
        } catch (FraudDetectedException e) {
            Transaction blocked = Transaction.blocked(source, amount, TransactionType.TRANSFER_OUT, e.getMessage());
            transactionRepository.save(blocked);
            throw e;
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(amount, source.getBalance());
        }

        source.debit(amount);
        target.credit(amount);
        accountRepository.save(source);
        accountRepository.save(target);

        // Record both sides of the transfer
        Transaction txOut = Transaction.transferOut(source, target, amount);
        Transaction txIn = Transaction.transferIn(source, target, amount);
        transactionRepository.save(txOut);
        transactionRepository.save(txIn);

        return txOut;
    }

    // =========================================================================
    // TRANSACTION HISTORY
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return transactionRepository.findByAccountId(accountId);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private void checkDailyLimit(Account account, BigDecimal amount) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        BigDecimal withdrawnLast24h = transactionRepository.sumWithdrawalsSince(
                account.getId(), twentyFourHoursAgo);

        BigDecimal remainingAllowance = account.getDailyLimit().subtract(withdrawnLast24h);

        if (amount.compareTo(remainingAllowance) > 0) {
            throw new DailyLimitExceededException(amount, remainingAllowance);
        }
    }
}
