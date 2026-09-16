package com.settleflow.settleflow;

import com.settleflow.entity.Transaction;
import com.settleflow.reconciliation.ExternalRecord;
import com.settleflow.reconciliation.ReconciliationMatcher;
import com.settleflow.reconciliation.ReconciliationResult;
import com.settleflow.reconciliation.ReconciliationStatus;
import com.settleflow.reconciliation.ReconciliationSummary;
import com.settleflow.reconciliation.ReconciliationSummaryCalculator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReconciliationMatcherTest {

    @Test
    void shouldReconcileMatchedMismatchAndOrphanRecords() {

        /*
         * ---------------------------------------------------------
         * STEP 1: Create internal transactions
         * ---------------------------------------------------------
         */

        Transaction matchedTransaction =
                createTransaction(
                        "REF-MATCHED",
                        "100.00"
                );

        Transaction mismatchTransaction =
                createTransaction(
                        "REF-MISMATCH",
                        "200.00"
                );

        Transaction internalOnlyTransaction =
                createTransaction(
                        "REF-INTERNAL-ONLY",
                        "700.00"
                );

        List<Transaction> internalTransactions =
                List.of(
                        matchedTransaction,
                        mismatchTransaction,
                        internalOnlyTransaction
                );

        /*
         * ---------------------------------------------------------
         * STEP 2: Create external bank records
         * ---------------------------------------------------------
         */

        ExternalRecord matchedExternalRecord =
                createExternalRecord(
                        "REF-MATCHED",
                        "100.00"
                );

        ExternalRecord mismatchExternalRecord =
                createExternalRecord(
                        "REF-MISMATCH",
                        "150.00"
                );

        ExternalRecord externalOnlyRecord =
                createExternalRecord(
                        "REF-EXTERNAL-ONLY",
                        "500.00"
                );

        List<ExternalRecord> externalRecords =
                List.of(
                        matchedExternalRecord,
                        mismatchExternalRecord,
                        externalOnlyRecord
                );

        /*
         * ---------------------------------------------------------
         * STEP 3: Run reconciliation
         * ---------------------------------------------------------
         */

        ReconciliationMatcher matcher =
                new ReconciliationMatcher();

        List<ReconciliationResult> results =
                matcher.reconcile(
                        internalTransactions,
                        externalRecords
                );

        /*
         * ---------------------------------------------------------
         * STEP 4: Verify total result count
         * ---------------------------------------------------------
         */

        assertEquals(
                4,
                results.size(),
                "All matched, mismatched and orphan records must be returned"
        );

        /*
         * ---------------------------------------------------------
         * STEP 5: Find each result
         * ---------------------------------------------------------
         */

        ReconciliationResult matchedResult =
                findResult(
                        results,
                        "REF-MATCHED"
                );

        ReconciliationResult mismatchResult =
                findResult(
                        results,
                        "REF-MISMATCH"
                );

        ReconciliationResult externalOnlyResult =
                findResult(
                        results,
                        "REF-EXTERNAL-ONLY"
                );

        ReconciliationResult internalOnlyResult =
                findResult(
                        results,
                        "REF-INTERNAL-ONLY"
                );

        /*
         * ---------------------------------------------------------
         * STEP 6: Verify MATCHED
         * ---------------------------------------------------------
         */

        assertEquals(
                ReconciliationStatus.MATCHED,
                matchedResult.getStatus()
        );

        assertNotNull(
                matchedResult.getInternalTransaction()
        );

        assertNotNull(
                matchedResult.getExternalRecord()
        );

        assertEquals(
                "REF-MATCHED",
                matchedResult.getReferenceId()
        );

        /*
         * ---------------------------------------------------------
         * STEP 7: Verify AMOUNT_MISMATCH
         * ---------------------------------------------------------
         */

        assertEquals(
                ReconciliationStatus.AMOUNT_MISMATCH,
                mismatchResult.getStatus()
        );

        assertNotNull(
                mismatchResult.getInternalTransaction()
        );

        assertNotNull(
                mismatchResult.getExternalRecord()
        );

        assertEquals(
                "REF-MISMATCH",
                mismatchResult.getReferenceId()
        );

        assertEquals(
                0,
                mismatchResult
                        .getInternalTransaction()
                        .getAmount()
                        .compareTo(new BigDecimal("200.00"))
        );

        assertEquals(
                0,
                mismatchResult
                        .getExternalRecord()
                        .getAmount()
                        .compareTo(new BigDecimal("150.00"))
        );

        /*
         * ---------------------------------------------------------
         * STEP 8: Verify EXTERNAL ORPHAN
         * ---------------------------------------------------------
         */

        assertEquals(
                ReconciliationStatus.UNMATCHED,
                externalOnlyResult.getStatus()
        );

        assertNull(
                externalOnlyResult.getInternalTransaction()
        );

        assertNotNull(
                externalOnlyResult.getExternalRecord()
        );

        assertEquals(
                "REF-EXTERNAL-ONLY",
                externalOnlyResult.getReferenceId()
        );

        /*
         * ---------------------------------------------------------
         * STEP 9: Verify INTERNAL ORPHAN
         * ---------------------------------------------------------
         */

        assertEquals(
                ReconciliationStatus.UNMATCHED,
                internalOnlyResult.getStatus()
        );

        assertNotNull(
                internalOnlyResult.getInternalTransaction()
        );

        assertNull(
                internalOnlyResult.getExternalRecord()
        );

        assertEquals(
                "REF-INTERNAL-ONLY",
                internalOnlyResult.getReferenceId()
        );
    }

    /*
     * -------------------------------------------------------------
     * Test: Reconciliation summary
     * -------------------------------------------------------------
     */

    @Test
    void shouldCalculateCorrectReconciliationSummary() {

        /*
         * Internal:
         *
         * REF-MATCHED        = 100.00
         * REF-MISMATCH       = 200.00
         * REF-INTERNAL-ONLY  = 700.00
         *
         * External:
         *
         * REF-MATCHED        = 100.00
         * REF-MISMATCH       = 150.00
         * REF-EXTERNAL-ONLY  = 500.00
         */

        Transaction matchedTransaction =
                createTransaction(
                        "REF-MATCHED",
                        "100.00"
                );

        Transaction mismatchTransaction =
                createTransaction(
                        "REF-MISMATCH",
                        "200.00"
                );

        Transaction internalOnlyTransaction =
                createTransaction(
                        "REF-INTERNAL-ONLY",
                        "700.00"
                );

        List<Transaction> internalTransactions =
                List.of(
                        matchedTransaction,
                        mismatchTransaction,
                        internalOnlyTransaction
                );

        ExternalRecord matchedExternalRecord =
                createExternalRecord(
                        "REF-MATCHED",
                        "100.00"
                );

        ExternalRecord mismatchExternalRecord =
                createExternalRecord(
                        "REF-MISMATCH",
                        "150.00"
                );

        ExternalRecord externalOnlyRecord =
                createExternalRecord(
                        "REF-EXTERNAL-ONLY",
                        "500.00"
                );

        List<ExternalRecord> externalRecords =
                List.of(
                        matchedExternalRecord,
                        mismatchExternalRecord,
                        externalOnlyRecord
                );

        /*
         * Run matcher
         */

        ReconciliationMatcher matcher =
                new ReconciliationMatcher();

        List<ReconciliationResult> results =
                matcher.reconcile(
                        internalTransactions,
                        externalRecords
                );

        /*
         * Calculate summary
         */

        ReconciliationSummaryCalculator calculator =
                new ReconciliationSummaryCalculator();

        ReconciliationSummary summary =
                calculator.calculate(results);

        /*
         * Expected:
         *
         * totalMatched         = 1
         * totalMismatched      = 1
         * totalUnmatched       = 2
         * totalValueReconciled = 100.00
         */

        assertEquals(
                1,
                summary.getTotalMatched()
        );

        assertEquals(
                1,
                summary.getTotalMismatched()
        );

        assertEquals(
                2,
                summary.getTotalUnmatched()
        );

        assertEquals(
                0,
                summary.getTotalValueReconciled()
                        .compareTo(new BigDecimal("100.00"))
        );
    }

    /*
     * -------------------------------------------------------------
     * Test: Partial match resolution
     * -------------------------------------------------------------
     *
     * Internal transaction:
     *
     * REF-PARTIAL = 100.00
     *
     * External records:
     *
     * REF-PARTIAL = 60.00
     * REF-PARTIAL = 40.00
     *
     * External total:
     *
     * 60.00 + 40.00 = 100.00
     *
     * Expected:
     *
     * PARTIAL_MATCH_RESOLVED
     *
     * NOT:
     *
     * AMOUNT_MISMATCH
     */

    @Test
    void shouldResolvePartialMatchWhenMultipleExternalRecordsSumToInternalAmount() {

        Transaction internalTransaction =
                createTransaction(
                        "REF-PARTIAL",
                        "100.00"
                );

        ExternalRecord firstExternalRecord =
                createExternalRecord(
                        "REF-PARTIAL",
                        "60.00"
                );

        ExternalRecord secondExternalRecord =
                createExternalRecord(
                        "REF-PARTIAL",
                        "40.00"
                );

        ReconciliationMatcher matcher =
                new ReconciliationMatcher();

        List<ReconciliationResult> results =
                matcher.reconcile(
                        List.of(internalTransaction),
                        List.of(
                                firstExternalRecord,
                                secondExternalRecord
                        )
                );

        ReconciliationResult result =
                findResult(
                        results,
                        "REF-PARTIAL"
                );

        /*
         * The combined external amount is:
         *
         * 60.00 + 40.00 = 100.00
         *
         * Therefore this is a resolved partial match.
         */

        assertEquals(
                ReconciliationStatus.PARTIAL_MATCH_RESOLVED,
                result.getStatus()
        );

        /*
         * Explicitly verify that it is NOT treated
         * as an amount mismatch.
         */

        assertNotEquals(
                ReconciliationStatus.AMOUNT_MISMATCH,
                result.getStatus()
        );

        assertEquals(
                "REF-PARTIAL",
                result.getReferenceId()
        );

        assertNotNull(
                result.getInternalTransaction()
        );

        assertNotNull(
                result.getExternalRecord()
        );
    }

    @Test
void shouldReturnAmountMismatchWhenMultipleExternalRecordsDoNotSumToInternalAmount() {

    Transaction internalTransaction =
            createTransaction(
                    "REF-PARTIAL-MISMATCH",
                    "100.00"
            );

    ExternalRecord firstExternalRecord =
            createExternalRecord(
                    "REF-PARTIAL-MISMATCH",
                    "60.00"
            );

    ExternalRecord secondExternalRecord =
            createExternalRecord(
                    "REF-PARTIAL-MISMATCH",
                    "30.00"
            );

    ReconciliationMatcher matcher =
            new ReconciliationMatcher();

    List<ReconciliationResult> results =
            matcher.reconcile(
                    List.of(internalTransaction),
                    List.of(
                            firstExternalRecord,
                            secondExternalRecord
                    )
            );

    ReconciliationResult result =
            findResult(
                    results,
                    "REF-PARTIAL-MISMATCH"
            );

    assertEquals(
            ReconciliationStatus.AMOUNT_MISMATCH,
            result.getStatus()
    );
}
    /*
     * -------------------------------------------------------------
     * Helper method: Create internal Transaction
     * -------------------------------------------------------------
     */

    private Transaction createTransaction(
            String referenceId,
            String amount) {

        Transaction transaction =
                new Transaction();

        transaction.setId(
                UUID.randomUUID()
        );

        transaction.setReferenceId(
                referenceId
        );

        transaction.setAmount(
                new BigDecimal(amount)
        );

        transaction.setMerchantId(
                "MERCHANT-001"
        );

        transaction.setAccountId(
                "ACCOUNT-001"
        );

        transaction.setStatus(
                "PENDING"
        );

        transaction.setIdempotencyKey(
                "reconciliation-test-"
                        + UUID.randomUUID()
        );

        transaction.setCreatedAt(
                LocalDateTime.now()
        );

        return transaction;
    }

    /*
     * -------------------------------------------------------------
     * Helper method: Create external record
     * -------------------------------------------------------------
     */

    private ExternalRecord createExternalRecord(
            String referenceId,
            String amount) {

        return new ExternalRecord(
                referenceId,
                new BigDecimal(amount),
                LocalDateTime.now()
        );
    }

    /*
     * -------------------------------------------------------------
     * Helper method: Find result by reference ID
     * -------------------------------------------------------------
     */

    private ReconciliationResult findResult(
            List<ReconciliationResult> results,
            String referenceId) {

        return results.stream()
                .filter(result ->
                        result.getReferenceId()
                                .equals(referenceId))
                .findFirst()
                .orElseThrow();
    }
}