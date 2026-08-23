package com.antoniobytedev.transference.worker;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.antoniobytedev.transference.service.AccountService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;

@Component
public class BalanceCheckWorker {

    private static final Logger LOG = LoggerFactory.getLogger(BalanceCheckWorker.class);

    private final AccountService accountService;


    public BalanceCheckWorker(AccountService accountService) {
        this.accountService = accountService;
    }

    public record BalanceCheckResult(boolean sufficientBalance, double availableBalance) {
    }

    @JobWorker(type = "balance-check")
    public BalanceCheckResult checkBalance(@Variable String fromAccount, @Variable double amount) {
        LOG.info("Checking balance for account {} against transfer amount {}", fromAccount, amount);

        double availableBalance = accountService.getBalance(fromAccount);
        boolean sufficient = availableBalance >= amount;

        return new BalanceCheckResult(sufficient, availableBalance);
    }
}