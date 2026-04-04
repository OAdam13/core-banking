package com.adam.banking.controller;

import com.adam.banking.dto.AccountResponse;
import com.adam.banking.dto.CreateAccountRequest;
import com.adam.banking.dto.DepositRequest;
import com.adam.banking.dto.TransactionResponse;
import com.adam.banking.dto.WithdrawRequest;
import com.adam.banking.entity.Account;
import com.adam.banking.entity.Transaction;
import com.adam.banking.service.AccountService;
import com.adam.banking.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.holderName(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> accounts = accountService.getAllAccounts().stream()
                .map(AccountResponse::from)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable Long id,
            @RequestBody DepositRequest request) {
        Transaction tx = transactionService.deposit(id, request.amount());
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable Long id,
            @RequestBody WithdrawRequest request) {
        Transaction tx = transactionService.withdraw(id, request.amount());
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long id) {
        List<TransactionResponse> transactions = transactionService.getTransactionHistory(id).stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(transactions);
    }
}
