package com.settleflow.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExternalRecord {

    private String batchId;

    private String referenceId;

    private BigDecimal amount;

    private LocalDateTime timestamp;

    public ExternalRecord() {
    }

    // Backward-compatible constructor
    public ExternalRecord(
            String referenceId,
            BigDecimal amount,
            LocalDateTime timestamp) {

        this.referenceId = referenceId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Batch-aware constructor
    public ExternalRecord(
            String batchId,
            String referenceId,
            BigDecimal amount,
            LocalDateTime timestamp) {

        this.batchId = batchId;
        this.referenceId = referenceId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
