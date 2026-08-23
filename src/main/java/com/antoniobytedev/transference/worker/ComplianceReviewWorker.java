package com.antoniobytedev.transference.worker;

import com.antoniobytedev.transference.service.PendingReviewRegistry;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ComplianceReviewWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ComplianceReviewWorker.class);

    private final PendingReviewRegistry pendingReviewRegistry;

    public ComplianceReviewWorker(PendingReviewRegistry pendingReviewRegistry) {
        this.pendingReviewRegistry = pendingReviewRegistry;
    }

    // autoComplete = false: this method registering the job is NOT the same as
    // completing it. The job stays open on the broker until submitDecision()
    // is called later (from ComplianceReviewController), when the frontend
    // sends an approve/reject decision.
    //
    // IMPORTANT: the default job activation timeout (a few minutes) is far too
    // short for a job that waits on a human decision. Set timeout explicitly
    // to something like 24 hours, or the broker will assume this worker died
    // and hand the job to another poller, causing duplicate registrations.
    @JobWorker(type = "compliance-review", autoComplete = false, timeout = 86_400_000L)
    public void awaitReview(
            final ActivatedJob job,
            @Variable String fromAccount,
            @Variable String toAccount,
            @Variable double amount,
            @Variable(name = "riskScore") Integer riskScore,
            @Variable(name = "reason") String reason
    ) {
        LOG.info(
                "Compliance review pending — job {} for transfer {} -> {} amount {} (risk {})",
                job.getKey(), fromAccount, toAccount, amount, riskScore
        );

        pendingReviewRegistry.register(job, fromAccount, toAccount, amount, riskScore, reason);
        // Do not complete the job here. It is completed later via PendingReviewRegistry.submitDecision().
    }
}
