package com.adam.banking.service;

import com.adam.banking.entity.Account;
import com.adam.banking.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class InterestService {

    private static final Logger log = LoggerFactory.getLogger(InterestService.class);

    // 0.01% daily ≈ 3.7% annually (compound)
    private static final BigDecimal DAILY_RATE = new BigDecimal("0.0001");

    private final AccountRepository accountRepository;

    public InterestService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Apply daily compound interest to all accounts with positive balance.
     * Formula: new_balance = balance * (1 + daily_rate)
     */
    @Transactional
    public int applyDailyInterest() {
        List<Account> accounts = accountRepository.findAll();
        int updated = 0;

        for (Account account : accounts) {
            if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal interest = account.getBalance()
                        .multiply(DAILY_RATE)
                        .setScale(2, RoundingMode.HALF_UP);

                account.setBalance(account.getBalance().add(interest));
                accountRepository.save(account);
                updated++;

                log.debug("Applied interest of {} to account {} (new balance: {})",
                        interest, account.getId(), account.getBalance());
            }
        }

        log.info("Daily interest applied to {} accounts", updated);
        return updated;
    }
}
