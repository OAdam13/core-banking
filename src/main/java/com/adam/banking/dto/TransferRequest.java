package com.adam.banking.dto;

import java.math.BigDecimal;

public record TransferRequest(
    Long sourceAccountId,
    Long targetAccountId,
    BigDecimal amount
) {}
