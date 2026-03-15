package com.adam.banking.repository;

import com.adam.banking.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * CRITICAL: This is the pessimistic lock query.
     *
     * It translates to: SELECT * FROM accounts WHERE id = ? FOR UPDATE
     *
     * Any other transaction trying to lock the same row will BLOCK
     * until this transaction commits or rolls back.
     *
     * This is what prevents the double-withdrawal race condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);

    boolean existsByEmail(String email);
}
