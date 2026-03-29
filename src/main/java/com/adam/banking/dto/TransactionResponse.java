package com.adam.banking.dto;

import com.adam.banking.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    BigDecimal amount,
    String type,
    String status,
    String description,
    Long sourceAccountId,
    Long targetAccountId,
    LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
            tx.getId(),
            tx.getAmount(),
            tx.getType().name(),
            tx.getStatus().name(),
            tx.getDescription(),
            tx.getSourceAccount() != null ? tx.getSourceAccount().getId() : null,
            tx.getTargetAccount() != null ? tx.getTargetAccount().getId() : null,
            tx.getCreatedAt()
        );
    }
}
