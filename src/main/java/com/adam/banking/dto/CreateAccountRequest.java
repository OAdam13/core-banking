package com.adam.banking.dto;

public record CreateAccountRequest(
    String holderName,
    String email
) {}
