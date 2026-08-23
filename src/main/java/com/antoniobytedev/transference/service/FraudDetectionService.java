package com.antoniobytedev.transference.service;

import com.antoniobytedev.transference.entity.TransferHistory;
import com.antoniobytedev.transference.repository.TransferHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FraudDetectionService {

    private static final int VELOCITY_WINDOW_MINUTES = 10;
    private static final int VELOCITY_MAX_TRANSFERS = 5;
    private static final double HIGH_AMOUNT_THRESHOLD = 1000.0;
    private static final double AVG_MULTIPLIER_THRESHOLD = 3.0; // 5x average = suspicious

    private final TransferHistoryRepository transferHistoryRepository;

    public FraudDetectionService(TransferHistoryRepository transferHistoryRepository) {
        this.transferHistoryRepository = transferHistoryRepository;
    }

    public record FraudResult(boolean flagged, String reason, int riskScore) {}

    public FraudResult evaluate(String fromAccount, String toAccount, double amount) {
        int riskScore = 0;
        StringBuilder reasons = new StringBuilder();

        // 1. Velocity check
        Instant windowStart = Instant.now().minus(VELOCITY_WINDOW_MINUTES, ChronoUnit.MINUTES);
        long recentCount = transferHistoryRepository.countByFromAccountAndCreatedAtAfter(fromAccount, windowStart);
        if (recentCount >= VELOCITY_MAX_TRANSFERS) {
            riskScore += 40;
            reasons.append("High transfer velocity; ");
        }

        // 2. Absolute high-amount threshold
        if (amount >= HIGH_AMOUNT_THRESHOLD) {
            riskScore += 30;
            reasons.append("Amount exceeds high-value threshold; ");
        }

        // 3. Unusual relative to account's typical transfer size
        List<TransferHistory> recentHistory = transferHistoryRepository.findTop20ByFromAccountOrderByCreatedAtDesc(fromAccount);
        if (!recentHistory.isEmpty()) {
            double avg = recentHistory.stream().mapToDouble(TransferHistory::getAmount).average().orElse(0);
            if (avg > 0 && amount >= avg * AVG_MULTIPLIER_THRESHOLD) {
                riskScore += 20;
                reasons.append("Amount far above account's typical transfer size; ");
            }
        }

        // 4. New payee
        boolean seenPayeeBefore = transferHistoryRepository.existsByFromAccountAndToAccount(fromAccount, toAccount);
        if (!seenPayeeBefore && amount >= HIGH_AMOUNT_THRESHOLD / 2) {
            riskScore += 15;
            reasons.append("First transfer to this payee combined with high amount; ");
        }

        boolean flagged = riskScore >= 50;

        // persist this transfer as part of history regardless of outcome
        transferHistoryRepository.save(new TransferHistory(fromAccount, toAccount, amount, Instant.now(), flagged));

        return new FraudResult(flagged, reasons.toString(), riskScore);
    }
}