package com.antoniobytedev.transference.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.antoniobytedev.transference.service.FraudDetectionService;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;

@Component
public class FraudDetectionWorker {
    
    private static final Logger LOG = LoggerFactory.getLogger(FraudDetectionWorker.class);
    private final FraudDetectionService fraudDetectionService;

    public FraudDetectionWorker(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    public record FraudCheckResult(boolean flagged, String reason, int riskScore) {}

    @JobWorker(type = "fraud-check")
    public FraudCheckResult checkFraud(@Variable String fromAccount, @Variable String toAccount, @Variable double amount) {
        LOG.info("Running fraud check for transfer {} -> {} amount {}", fromAccount, toAccount, amount);

        var result = fraudDetectionService.evaluate(fromAccount, toAccount, amount);
        return new FraudCheckResult(result.flagged(), result.reason(), result.riskScore());
    }
}
