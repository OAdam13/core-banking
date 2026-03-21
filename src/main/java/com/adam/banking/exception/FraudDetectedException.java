package com.adam.banking.exception;

public class FraudDetectedException extends RuntimeException {
    public FraudDetectedException(String reason) {
        super("Transaction blocked — suspected fraud: " + reason);
    }
}
