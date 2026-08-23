package com.antoniobytedev.transference.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transfer_history")
public class TransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromAccount;
    private String toAccount;
    private double amount;
    private Instant createdAt;
    private boolean flagged;

    // constructors, getters, setters
    public TransferHistory() {}

    public TransferHistory(String fromAccount, String toAccount, double amount, Instant createdAt, boolean flagged) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.createdAt = createdAt;
        this.flagged = flagged;
    }

    public Long getId() { return id; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public double getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
}