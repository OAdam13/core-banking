package com.adam.banking.entity;

import com.adam.banking.enums.TransactionStatus;
import com.adam.banking.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Constructors ---

    public Transaction() {}

    // --- Static Factory Methods (cleaner than giant constructors) ---

    public static Transaction deposit(Account target, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.amount = amount;
        tx.type = TransactionType.DEPOSIT;
        tx.status = TransactionStatus.SUCCESS;
        tx.targetAccount = target;
        tx.description = "Deposit of " + amount;
        return tx;
    }

    public static Transaction withdrawal(Account source, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.amount = amount;
        tx.type = TransactionType.WITHDRAWAL;
        tx.status = TransactionStatus.SUCCESS;
        tx.sourceAccount = source;
        tx.description = "Withdrawal of " + amount;
        return tx;
    }

    public static Transaction transferOut(Account source, Account target, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.amount = amount;
        tx.type = TransactionType.TRANSFER_OUT;
        tx.status = TransactionStatus.SUCCESS;
        tx.sourceAccount = source;
        tx.targetAccount = target;
        tx.description = "Transfer to account #" + target.getId();
        return tx;
    }

    public static Transaction transferIn(Account source, Account target, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.amount = amount;
        tx.type = TransactionType.TRANSFER_IN;
        tx.status = TransactionStatus.SUCCESS;
        tx.sourceAccount = source;
        tx.targetAccount = target;
        tx.description = "Transfer from account #" + source.getId();
        return tx;
    }

    public static Transaction blocked(Account source, BigDecimal amount, TransactionType type, String reason) {
        Transaction tx = new Transaction();
        tx.amount = amount;
        tx.type = type;
        tx.status = TransactionStatus.BLOCKED;
        tx.sourceAccount = source;
        tx.description = "BLOCKED: " + reason;
        return tx;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public Account getSourceAccount() { return sourceAccount; }
    public Account getTargetAccount() { return targetAccount; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(TransactionStatus status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
}
