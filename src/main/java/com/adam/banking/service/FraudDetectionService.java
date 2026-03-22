package com.adam.banking.service;

import com.adam.banking.entity.Account;
import com.adam.banking.entity.Transaction;
import com.adam.banking.exception.FraudDetectedException;
import com.adam.banking.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;

    // Thresholds — in production, these would be externalized to application.yml
    private static final int MAX_TRANSACTIONS_PER_MINUTE = 5;
    private static final BigDecimal LARGE_DEPOSIT_THRESHOLD = new BigDecimal("1000.00");
    private static final int DEPOSIT_WITHDRAW_WINDOW_MINUTES = 5;

    public FraudDetectionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Run all fraud checks before executing a withdrawal or transfer.
     * Throws FraudDetectedException if any check fails.
     */
    public void checkForFraud(Account account, BigDecimal amount) {
        checkLargePercentageOfBalance(account, amount);
        checkTransactionVelocity(account);
        checkDepositThenWithdrawPattern(account);
    }

    /**
     * Rule 1: Single transaction > 50% of account balance.
     * A legitimate user rarely withdraws more than half their balance at once.
     */
    private void checkLargePercentageOfBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal halfBalance = account.getBalance().divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
            if (amount.compareTo(halfBalance) > 0) {
                throw new FraudDetectedException(
                    "Transaction amount (" + amount + ") exceeds 50% of account balance (" + account.getBalance() + ")"
                );
            }
        }
    }

    /**
     * Rule 2: More than 5 transactions in 1 minute from the same account.
     * Rapid-fire transactions suggest automated fraud or a compromised account.
     */
    private void checkTransactionVelocity(Account account) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentCount = transactionRepository.countTransactionsSince(account.getId(), oneMinuteAgo);
        if (recentCount >= MAX_TRANSACTIONS_PER_MINUTE) {
            throw new FraudDetectedException(
                "Too many transactions: " + recentCount + " in the last minute (max " + MAX_TRANSACTIONS_PER_MINUTE + ")"
            );
        }
    }

    /**
     * Rule 3: Withdrawal immediately after a large deposit.
     * Classic money laundering pattern: deposit dirty money, withdraw clean money.
     */
    private void checkDepositThenWithdrawPattern(Account account) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(DEPOSIT_WITHDRAW_WINDOW_MINUTES);
        List<Transaction> recentDeposits = transactionRepository.findRecentDeposits(account.getId(), windowStart);

        boolean hasRecentLargeDeposit = recentDeposits.stream()
                .anyMatch(d -> d.getAmount().compareTo(LARGE_DEPOSIT_THRESHOLD) >= 0);

        if (hasRecentLargeDeposit) {
            throw new FraudDetectedException(
                "Withdrawal attempted within " + DEPOSIT_WITHDRAW_WINDOW_MINUTES +
                " minutes of a large deposit (>=" + LARGE_DEPOSIT_THRESHOLD + ")"
            );
        }
    }
}
