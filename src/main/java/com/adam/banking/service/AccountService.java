package com.adam.banking.service;

import com.adam.banking.entity.Account;
import com.adam.banking.exception.AccountNotFoundException;
import com.adam.banking.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account createAccount(String holderName, String email) {
        if (accountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        Account account = new Account(holderName, email);
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }
}
