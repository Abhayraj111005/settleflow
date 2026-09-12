package com.settleflow.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExternalRecord {

    private String referenceId;

    private BigDecimal amount;

    private LocalDateTime timestamp;

    public ExternalRecord() {
    }

    public ExternalRecord(
            String referenceId,
            BigDecimal amount,
            LocalDateTime timestamp) {

        this.referenceId = referenceId;
        this.amount = amount;
        this.timestamp = timestamp;
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
