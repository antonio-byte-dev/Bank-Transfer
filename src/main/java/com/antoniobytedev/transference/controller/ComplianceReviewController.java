package com.antoniobytedev.transference.controller;

import com.antoniobytedev.transference.service.PendingReviewRegistry;
import com.antoniobytedev.transference.service.PendingReviewRegistry.PendingReview;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/reviews")
public class ComplianceReviewController {

    private final PendingReviewRegistry pendingReviewRegistry;

    public ComplianceReviewController(PendingReviewRegistry pendingReviewRegistry) {
        this.pendingReviewRegistry = pendingReviewRegistry;
    }

    public record DecisionRequest(boolean approved, String reviewer, String notes) {}

    // GET /reviews -> list everything currently waiting on a human decision
    @GetMapping
    public Collection<PendingReview> listPending() {
        return pendingReviewRegistry.listPending();
    }

    // POST /reviews/{jobKey}/decision -> frontend calls this to approve/reject
    @PostMapping("/{jobKey}/decision")
    public void submitDecision(@PathVariable long jobKey, @RequestBody DecisionRequest request) {
        pendingReviewRegistry.submitDecision(jobKey, request.approved(), request.reviewer(), request.notes());
    }
}
