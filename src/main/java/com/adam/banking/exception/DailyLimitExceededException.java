package com.adam.banking.exception;

import java.math.BigDecimal;

public class DailyLimitExceededException extends RuntimeException {
    public DailyLimitExceededException(BigDecimal requested, BigDecimal remaining) {
        super("Daily limit exceeded: requested " + requested + ", remaining allowance " + remaining);
    }
}
