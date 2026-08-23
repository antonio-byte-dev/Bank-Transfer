package com.antoniobytedev.transference.repository;

import com.antoniobytedev.transference.entity.TransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {

    // velocity check: how many transfers from this account since some timestamp
    long countByFromAccountAndCreatedAtAfter(String fromAccount, Instant since);

    // new payee check: has this account ever sent to this destination before
    boolean existsByFromAccountAndToAccount(String fromAccount, String toAccount);

    // for average amount comparison
    List<TransferHistory> findTop20ByFromAccountOrderByCreatedAtDesc(String fromAccount);
}