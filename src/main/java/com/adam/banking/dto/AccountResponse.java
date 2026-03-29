package com.adam.banking.dto;

import com.adam.banking.entity.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
    Long id,
    String holderName,
    String email,
    BigDecimal balance,
    BigDecimal dailyLimit,
    LocalDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getHolderName(),
            account.getEmail(),
            account.getBalance(),
            account.getDailyLimit(),
            account.getCreatedAt()
        );
    }
}
