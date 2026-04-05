package com.adam.banking.scheduler;

import com.adam.banking.service.InterestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InterestScheduler {

    private static final Logger log = LoggerFactory.getLogger(InterestScheduler.class);

    private final InterestService interestService;

    public InterestScheduler(InterestService interestService) {
        this.interestService = interestService;
    }

    /**
     * Runs every day at 2:00 AM.
     * Cron format: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleDailyInterest() {
        log.info("Starting daily interest calculation...");
        int count = interestService.applyDailyInterest();
        log.info("Daily interest calculation completed. {} accounts updated.", count);
    }
}
