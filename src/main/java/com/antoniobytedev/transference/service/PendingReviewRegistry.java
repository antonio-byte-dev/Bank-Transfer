package com.antoniobytedev.transference.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingReviewRegistry {

    public record PendingReview(
            long jobKey,
            String fromAccount,
            String toAccount,
            double amount,
            Integer riskScore,
            String reason,
            Instant receivedAt
    ) {}

    // NOTE: in-memory only. A restart of transfer-backend loses track of any
    // jobs currently awaiting review — they'll still exist on the Zeebe broker
    // (and the timeout above keeps them from being reassigned quickly), but
    // this app won't know about them until re-registered. For anything beyond
    // a demo, persist jobKey + variables to a DB table instead of this map.
    private final Map<Long, ActivatedJob> jobsByKey = new ConcurrentHashMap<>();
    private final Map<Long, PendingReview> detailsByKey = new ConcurrentHashMap<>();

    private final CamundaClient camundaClient;

    public PendingReviewRegistry(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    public void register(ActivatedJob job, String fromAccount, String toAccount,
                          double amount, Integer riskScore, String reason) {
        jobsByKey.put(job.getKey(), job);
        detailsByKey.put(job.getKey(), new PendingReview(
                job.getKey(), fromAccount, toAccount, amount, riskScore, reason, Instant.now()
        ));
    }

    public Collection<PendingReview> listPending() {
        return detailsByKey.values();
    }

    public void submitDecision(long jobKey, boolean approved, String reviewer, String notes) {
        ActivatedJob job = jobsByKey.get(jobKey);
        if (job == null) {
            throw new IllegalArgumentException("No pending review found for job " + jobKey);
        }

        camundaClient.newCompleteCommand(job.getKey())
                .variables(Map.of(
                        "complianceApproved", approved,
                        "reviewer", reviewer == null ? "" : reviewer,
                        "reviewNotes", notes == null ? "" : notes
                ))
                .send()
                .join();

        jobsByKey.remove(jobKey);
        detailsByKey.remove(jobKey);
    }
}
