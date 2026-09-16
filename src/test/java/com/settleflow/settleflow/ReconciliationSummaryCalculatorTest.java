package com.settleflow.settleflow;

import com.settleflow.entity.Transaction;
import com.settleflow.reconciliation.ReconciliationResult;
import com.settleflow.reconciliation.ReconciliationStatus;
import com.settleflow.reconciliation.ReconciliationSummary;
import com.settleflow.reconciliation.ReconciliationSummaryCalculator;
import com.settleflow.reconciliation.ExternalRecord;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;



import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconciliationSummaryCalculatorTest {

    private final ReconciliationSummaryCalculator calculator =
            new ReconciliationSummaryCalculator();

    @Test
    void shouldCalculateCorrectSummaryForMockBatch() {

        Transaction matchedTransaction =
                createTransaction("REF-MATCHED", "100.00");

        Transaction mismatchTransaction =
                createTransaction("REF-MISMATCH", "200.00");

        Transaction internalOnlyTransaction =
                createTransaction("REF-INTERNAL-ONLY", "700.00");

        ExternalRecord matchedExternal =
                new ExternalRecord(
                        "REF-MATCHED",
                        new BigDecimal("100.00"),
                        LocalDateTime.now()
                );

        ExternalRecord mismatchExternal =
                new ExternalRecord(
                        "REF-MISMATCH",
                        new BigDecimal("150.00"),
                        LocalDateTime.now()
                );

        ExternalRecord externalOnly =
                new ExternalRecord(
                        "REF-EXTERNAL-ONLY",
                        new BigDecimal("500.00"),
                        LocalDateTime.now()
                );

        List<ReconciliationResult> results = List.of(

                createResult(
                        "REF-MATCHED",
                        matchedTransaction,
                        matchedExternal,
                        ReconciliationStatus.MATCHED
                ),

                createResult(
                        "REF-MISMATCH",
                        mismatchTransaction,
                        mismatchExternal,
                        ReconciliationStatus.AMOUNT_MISMATCH
                ),

                createResult(
                        "REF-INTERNAL-ONLY",
                        internalOnlyTransaction,
                        null,
                        ReconciliationStatus.UNMATCHED
                ),

                createResult(
                        "REF-EXTERNAL-ONLY",
                        null,
                        externalOnly,
                        ReconciliationStatus.UNMATCHED
                )
        );

        ReconciliationSummary summary =
                calculator.calculate(results);

        // Manually calculated expected result:
        // 1 matched
        // 1 mismatched
        // 2 unmatched
        // 100.00 successfully reconciled
        assertEquals(1, summary.getTotalMatched());
        assertEquals(1, summary.getTotalMismatched());
        assertEquals(2, summary.getTotalUnmatched());
        assertEquals(
                new BigDecimal("100.00"),
                summary.getTotalValueReconciled()
        );
    }

    @Test
void shouldIncludePartialMatchResolvedInReconciledValue() {

    Transaction internalTransaction =
            createTransaction(
                    "REF-PARTIAL",
                    "100.00"
            );

    ExternalRecord firstExternal =
            new ExternalRecord(
                    "REF-PARTIAL",
                    new BigDecimal("60.00"),
                    LocalDateTime.now()
            );

    ExternalRecord secondExternal =
            new ExternalRecord(
                    "REF-PARTIAL",
                    new BigDecimal("40.00"),
                    LocalDateTime.now()
            );

    ReconciliationResult partialResult =
            createResult(
                    "REF-PARTIAL",
                    internalTransaction,
                    firstExternal,
                    ReconciliationStatus.PARTIAL_MATCH_RESOLVED
            );

    ReconciliationSummary summary =
            calculator.calculate(
                    List.of(partialResult)
            );

    assertEquals(
            0,
            summary.getTotalMatched()
    );

    assertEquals(
            0,
            summary.getTotalMismatched()
    );

    assertEquals(
            0,
            summary.getTotalUnmatched()
    );

    assertEquals(
            new BigDecimal("100.00"),
            summary.getTotalValueReconciled()
    );
}

    private ReconciliationResult createResult(
            String referenceId,
            Transaction internalTransaction,
            ExternalRecord externalRecord,
            ReconciliationStatus status) {

        ReconciliationResult result = new ReconciliationResult();

        result.setReferenceId(referenceId);
        result.setInternalTransaction(internalTransaction);
        result.setExternalRecord(externalRecord);
        result.setStatus(status);

        return result;
    }

    private Transaction createTransaction(
            String referenceId,
            String amount) {

        Transaction transaction = new Transaction();

        transaction.setId(UUID.randomUUID());
        transaction.setReferenceId(referenceId);
        transaction.setMerchantId("MERCHANT-001");
        transaction.setAccountId("ACCOUNT-001");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setStatus("PENDING");
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        transaction.setCreatedAt(LocalDateTime.now());

        return transaction;
    }
}