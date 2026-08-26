package com.antoniobytedev.transference.service;

import com.antoniobytedev.transference.entity.TransferHistory;
import com.antoniobytedev.transference.repository.TransferHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private TransferHistoryRepository transferHistoryRepository;

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        fraudDetectionService = new FraudDetectionService(transferHistoryRepository);
        // Default: no signals present unless a specific test overrides them
        lenient().when(transferHistoryRepository.countByFromAccountAndCreatedAtAfter(anyString(), any()))
                .thenReturn(0L);
        lenient().when(transferHistoryRepository.findTop20ByFromAccountOrderByCreatedAtDesc(anyString()))
                .thenReturn(List.of());
        lenient().when(transferHistoryRepository.existsByFromAccountAndToAccount(anyString(), anyString()))
                .thenReturn(true); // treat as a known payee unless a test says otherwise
    }

    @Test
    void evaluate_flagsNothingForAnOrdinaryTransfer() {
        FraudDetectionService.FraudResult result =
                fraudDetectionService.evaluate("ACC-1", "ACC-2", 100.0);

        assertThat(result.flagged()).isFalse();
        assertThat(result.riskScore()).isZero();
        verify(transferHistoryRepository).save(any(TransferHistory.class));
    }

    @Test
    void evaluate_flagsHighVelocity() {
        when(transferHistoryRepository.countByFromAccountAndCreatedAtAfter(eq("ACC-1"), any()))
                .thenReturn(5L); // at the configured threshold

        FraudDetectionService.FraudResult result =
                fraudDetectionService.evaluate("ACC-1", "ACC-2", 50.0);

        assertThat(result.riskScore()).isGreaterThanOrEqualTo(40);
        assertThat(result.reason()).contains("velocity");
    }

    @Test
    void evaluate_flagsHighAbsoluteAmount() {
        FraudDetectionService.FraudResult result =
                fraudDetectionService.evaluate("ACC-1", "ACC-2", 15_000.0);

        assertThat(result.riskScore()).isGreaterThanOrEqualTo(30);
        assertThat(result.reason()).contains("high-value threshold");
    }

    @Test
    void evaluate_flagsAmountFarAboveAccountAverage() {
        List<TransferHistory> history = List.of(
                new TransferHistory("ACC-1", "ACC-9", 20.0, Instant.now().minus(1, ChronoUnit.DAYS), false),
                new TransferHistory("ACC-1", "ACC-9", 25.0, Instant.now().minus(2, ChronoUnit.DAYS), false)
        );
        when(transferHistoryRepository.findTop20ByFromAccountOrderByCreatedAtDesc("ACC-1"))
                .thenReturn(history);

        // average is ~22.5, this transfer is far more than 5x that
        FraudDetectionService.FraudResult result =
                fraudDetectionService.evaluate("ACC-1", "ACC-2", 500.0);

        assertThat(result.riskScore()).isGreaterThanOrEqualTo(20);
        assertThat(result.reason()).contains("typical transfer size");
    }

    @Test
    void evaluate_flagsNewPayeeCombinedWithHighAmount() {
        when(transferHistoryRepository.existsByFromAccountAndToAccount("ACC-1", "ACC-2"))
                .thenReturn(false);

        FraudDetectionService.FraudResult result =
                fraudDetectionService.evaluate("ACC-1", "ACC-2", 6_000.0);

        assertThat(result.riskScore()).isGreaterThanOrEqualTo(15);
        assertThat(result.reason()).contains("payee");
    }

    @Test
    void evaluate_doesNotFlagNewPayeeWhenAmountIsLow() {
        when(transferHistoryRepository.existsByFromAccountAndToAccount("ACC-1", "ACC-2"))
                .thenReturn(false);

        FraudDetectionService.FraudResult result =
                fraudDetectionService.evaluate("ACC-1", "ACC-2", 20.0);

        assertThat(result.reason()).doesNotContain("payee");
    }

    @Test
    void evaluate_combinesMultipleSignalsIntoFlaggedResult() {
        when(transferHistoryRepository.countByFromAccountAndCreatedAtAfter(eq("ACC-1"), any()))
                .thenReturn(6L);
        when(transferHistoryRepository.existsByFromAccountAndToAccount("ACC-1", "ACC-2"))
                .thenReturn(false);

        FraudDetectionService.FraudResult result =
                fraudDetectionService.evaluate("ACC-1", "ACC-2", 12_000.0);

        // velocity (40) + high amount (30) + new payee (15) = 85, well past the 50 flag threshold
        assertThat(result.flagged()).isTrue();
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(50);
    }

    @Test
    void evaluate_alwaysSavesTransferHistoryRegardlessOfOutcome() {
        fraudDetectionService.evaluate("ACC-1", "ACC-2", 10.0);

        verify(transferHistoryRepository).save(argThat(record ->
                record.getFromAccount().equals("ACC-1")
                        && record.getToAccount().equals("ACC-2")
                        && record.getAmount() == 10.0
        ));
    }
}
