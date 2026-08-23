-- Migration: create transfer_history table
-- Required by FraudDetectionService for velocity, new-payee, and average-amount checks

CREATE TABLE IF NOT EXISTS transfer_history (
    id           BIGSERIAL PRIMARY KEY,
    from_account VARCHAR(255) NOT NULL,
    to_account   VARCHAR(255) NOT NULL,
    amount       DOUBLE PRECISION NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    flagged      BOOLEAN NOT NULL DEFAULT FALSE
);

-- Speeds up velocity checks (countByFromAccountAndCreatedAtAfter)
CREATE INDEX IF NOT EXISTS idx_transfer_history_from_account_created_at
    ON transfer_history (from_account, created_at);

-- Speeds up new-payee checks (existsByFromAccountAndToAccount)
CREATE INDEX IF NOT EXISTS idx_transfer_history_from_to
    ON transfer_history (from_account, to_account);

-- Speeds up average-amount lookups (findTop20ByFromAccountOrderByCreatedAtDesc)
CREATE INDEX IF NOT EXISTS idx_transfer_history_from_account_created_at_desc
    ON transfer_history (from_account, created_at DESC);