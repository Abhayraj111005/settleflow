package com.settleflow.reconciliation;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ReconciliationSummaryCalculator {

    public ReconciliationSummary calculate(
            List<ReconciliationResult> results) {

        ReconciliationSummary summary = new ReconciliationSummary();

        BigDecimal totalValueReconciled = BigDecimal.ZERO;

        for (ReconciliationResult result : results) {

           switch (result.getStatus()) {

    case MATCHED:
        summary.setTotalMatched(
                summary.getTotalMatched() + 1
        );

        totalValueReconciled = totalValueReconciled.add(
                result.getInternalTransaction().getAmount()
        );
        break;

    case PARTIAL_MATCH_RESOLVED:
        totalValueReconciled = totalValueReconciled.add(
                result.getInternalTransaction().getAmount()
        );
        break;

    case AMOUNT_MISMATCH:
        summary.setTotalMismatched(
                summary.getTotalMismatched() + 1
        );
        break;

    case UNMATCHED:
        summary.setTotalUnmatched(
                summary.getTotalUnmatched() + 1
        );
        break;
}
        }

        summary.setTotalValueReconciled(totalValueReconciled);

        return summary;
    }
}