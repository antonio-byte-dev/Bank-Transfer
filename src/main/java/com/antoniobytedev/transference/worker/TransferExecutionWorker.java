package com.antoniobytedev.transference.worker;

import com.antoniobytedev.transference.service.AccountService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TransferExecutionWorker {

    private static final Logger LOG = LoggerFactory.getLogger(TransferExecutionWorker.class);

    private final AccountService accountService;

    public TransferExecutionWorker(AccountService accountService) {
        this.accountService = accountService;
    }

    public record TransferExecutionResult(boolean success, String message) {}

    // This is intended as the final service task in the flow, after
    // balance-check, fraud-check, and (if flagged) compliance-review have
    // all passed. If executeTransfer() throws, the job fails and Zeebe
    // raises an incident — that's the desired behavior here, since a failed
    // balance update should stop the process and surface for investigation
    // rather than silently continuing.
    @JobWorker(type = "execute-transfer")
    public TransferExecutionResult executeTransfer(
            @Variable String fromAccount,
            @Variable String toAccount,
            @Variable double amount
    ) {
        LOG.info("Executing transfer {} -> {} amount {}", fromAccount, toAccount, amount);

        accountService.executeTransfer(fromAccount, toAccount, amount);

        LOG.info("Transfer completed {} -> {} amount {}", fromAccount, toAccount, amount);
        return new TransferExecutionResult(true, "Transfer completed");
    }
}