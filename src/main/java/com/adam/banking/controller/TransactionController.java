package com.adam.banking.controller;

import com.adam.banking.dto.TransactionResponse;
import com.adam.banking.dto.TransferRequest;
import com.adam.banking.entity.Transaction;
import com.adam.banking.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransferRequest request) {
        Transaction tx = transactionService.transfer(
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount()
        );
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }
}
