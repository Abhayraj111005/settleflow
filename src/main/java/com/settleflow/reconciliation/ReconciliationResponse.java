package com.settleflow.reconciliation;

import java.util.List;

public class ReconciliationResponse {

    private ReconciliationSummary summary;
    private List<ReconciliationResult> results;

    public ReconciliationResponse() {
    }

    public ReconciliationResponse(
            ReconciliationSummary summary,
            List<ReconciliationResult> results) {

        this.summary = summary;
        this.results = results;
    }

    public ReconciliationSummary getSummary() {
        return summary;
    }

    public void setSummary(ReconciliationSummary summary) {
        this.summary = summary;
    }

    public List<ReconciliationResult> getResults() {
        return results;
    }

    public void setResults(List<ReconciliationResult> results) {
        this.results = results;
    }
}
