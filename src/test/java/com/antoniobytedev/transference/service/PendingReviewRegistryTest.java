package com.antoniobytedev.transference.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PendingReviewRegistryTest {

    // Deep stubs so we don't need to know every intermediate builder type in
    // the newCompleteCommand(...).variables(...).send().join() chain — if any
    // method name in that chain differs for your camunda-client version, this
    // test will fail to compile at that call site and point you to the fix.
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CamundaClient camundaClient;

    @Mock
    private ActivatedJob job;

    private PendingReviewRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PendingReviewRegistry(camundaClient);
        lenient().when(job.getKey()).thenReturn(42L);
    }

    @Test
    void register_addsReviewToThePendingList() {
        registry.register(job, "ACC-1", "ACC-2", 500.0, 65, "High risk score");

        Collection<PendingReviewRegistry.PendingReview> pending = registry.listPending();

        assertThat(pending).hasSize(1);
        PendingReviewRegistry.PendingReview review = pending.iterator().next();
        assertThat(review.jobKey()).isEqualTo(42L);
        assertThat(review.fromAccount()).isEqualTo("ACC-1");
        assertThat(review.toAccount()).isEqualTo("ACC-2");
        assertThat(review.amount()).isEqualTo(500.0);
        assertThat(review.riskScore()).isEqualTo(65);
        assertThat(review.reason()).isEqualTo("High risk score");
    }

    @Test
    void submitDecision_completesTheJobAndRemovesFromPendingList() {
        registry.register(job, "ACC-1", "ACC-2", 500.0, 65, "High risk score");

        registry.submitDecision(42L, true, "admin", "looks fine");

        verify(camundaClient).newCompleteCommand(42L);
        assertThat(registry.listPending()).isEmpty();
    }

    @Test
    void submitDecision_throwsWhenJobKeyIsUnknown() {
        assertThatThrownBy(() -> registry.submitDecision(999L, true, "admin", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }
}
