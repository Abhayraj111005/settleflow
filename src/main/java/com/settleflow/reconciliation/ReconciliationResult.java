package com.settleflow.reconciliation;

import com.settleflow.entity.Transaction;

public class ReconciliationResult {

    private String referenceId;
    private Transaction internalTransaction;
    private ExternalRecord externalRecord;
    private ReconciliationStatus status;

    public ReconciliationResult() {
    }

    public ReconciliationResult(
            String referenceId,
            Transaction internalTransaction,
            ExternalRecord externalRecord,
            ReconciliationStatus status) {

        this.referenceId = referenceId;
        this.internalTransaction = internalTransaction;
        this.externalRecord = externalRecord;
        this.status = status;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Transaction getInternalTransaction() {
        return internalTransaction;
    }

    public void setInternalTransaction(Transaction internalTransaction) {
        this.internalTransaction = internalTransaction;
    }

    public ExternalRecord getExternalRecord() {
        return externalRecord;
    }

    public void setExternalRecord(ExternalRecord externalRecord) {
        this.externalRecord = externalRecord;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }
}