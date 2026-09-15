package com.settleflow.reconciliation;

import java.math.BigDecimal;

public class ReconciliationSummary {

    private long totalMatched;
    private long totalMismatched;
    private long totalUnmatched;
    private BigDecimal totalValueReconciled;

    public ReconciliationSummary() {
        this.totalValueReconciled = BigDecimal.ZERO;
    }

    public ReconciliationSummary(
            long totalMatched,
            long totalMismatched,
            long totalUnmatched,
            BigDecimal totalValueReconciled) {

        this.totalMatched = totalMatched;
        this.totalMismatched = totalMismatched;
        this.totalUnmatched = totalUnmatched;
        this.totalValueReconciled = totalValueReconciled;
    }

    public long getTotalMatched() {
        return totalMatched;
    }

    public void setTotalMatched(long totalMatched) {
        this.totalMatched = totalMatched;
    }

    public long getTotalMismatched() {
        return totalMismatched;
    }

    public void setTotalMismatched(long totalMismatched) {
        this.totalMismatched = totalMismatched;
    }

    public long getTotalUnmatched() {
        return totalUnmatched;
    }

    public void setTotalUnmatched(long totalUnmatched) {
        this.totalUnmatched = totalUnmatched;
    }

    public BigDecimal getTotalValueReconciled() {
        return totalValueReconciled;
    }

    public void setTotalValueReconciled(
            BigDecimal totalValueReconciled) {

        this.totalValueReconciled = totalValueReconciled;
    }
}