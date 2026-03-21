package com.adam.banking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(AccountNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleDailyLimit(DailyLimitExceededException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(FraudDetectedException.class)
    public ResponseEntity<Map<String, Object>> handleFraud(FraudDetectedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "message", message
        );
        return new ResponseEntity<>(body, status);
    }
}
