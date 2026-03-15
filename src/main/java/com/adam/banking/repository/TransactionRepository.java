package com.adam.banking.repository;

import com.adam.banking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Get all transactions for an account (as source OR target), ordered by most recent first.
     */
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Calculate total withdrawn in the last 24 hours (sliding window).
     * Used for daily limit enforcement.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.sourceAccount.id = :accountId " +
           "AND t.type IN ('WITHDRAWAL', 'TRANSFER_OUT') " +
           "AND t.status = 'SUCCESS' " +
           "AND t.createdAt >= :since")
    BigDecimal sumWithdrawalsSince(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);

    /**
     * Count recent transactions from an account (fraud detection: velocity check).
     */
    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.sourceAccount.id = :accountId " +
           "AND t.createdAt >= :since")
    long countTransactionsSince(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);

    /**
     * Find recent deposits to an account (fraud detection: deposit-then-withdraw pattern).
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.targetAccount.id = :accountId " +
           "AND t.type = 'DEPOSIT' " +
           "AND t.status = 'SUCCESS' " +
           "AND t.createdAt >= :since " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findRecentDeposits(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);
}
