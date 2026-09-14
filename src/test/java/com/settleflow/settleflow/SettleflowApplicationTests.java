package com.settleflow.settleflow;

import com.settleflow.entity.LedgerEntry;
import com.settleflow.entity.LedgerEntryType;
import com.settleflow.entity.ReconciliationException;
import com.settleflow.entity.Settlement;
import com.settleflow.entity.Transaction;
import com.settleflow.repository.LedgerEntryRepository;
import com.settleflow.repository.ReconciliationExceptionRepository;
import com.settleflow.repository.SettlementRepository;
import com.settleflow.repository.TransactionRepository;
import com.settleflow.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class SettleflowApplicationTests {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReconciliationExceptionRepository reconciliationExceptionRepository;


    @BeforeEach
    void beforeEach() {
        /*
         * Tests intentionally use unique IDs/reference IDs.
         *
         * We do not delete shared project data here because some tests
         * verify rollback and persistence behavior.
         */
    }


    @Test
    void onlyOneProcessedEventShouldBeCreatedForConcurrentSameEventId()
            throws Exception {

        /*
         * KEEP YOUR EXISTING CONCURRENCY TEST IMPLEMENTATION HERE.
         *
         * The uploaded version of this file contained only a placeholder
         * for this test, so the original implementation cannot safely be
         * reconstructed without inventing code.
         */
    }


    @Test
    void creatingTransactionShouldCreateBalancedLedgerPair() {

        Settlement settlement = new Settlement();

        settlement.setId(
                UUID.randomUUID()
        );

        settlement.setMerchantId(
                "MERCHANT-LEDGER-" + UUID.randomUUID()
        );

        settlement.setAmount(
                new BigDecimal("100.00")
        );

        settlement.setCreatedAt(
                LocalDateTime.now()
        );

        settlement =
                settlementRepository.saveAndFlush(
                        settlement
                );


        Transaction transaction = new Transaction();

        transaction.setId(
                UUID.randomUUID()
        );

        transaction.setSettlementId(
                settlement.getId()
        );

        transaction.setMerchantId(
                settlement.getMerchantId()
        );

        transaction.setAccountId(
                "ACCOUNT-LEDGER-" + UUID.randomUUID()
        );

        transaction.setAmount(
                new BigDecimal("100.00")
        );

        transaction.setStatus("PENDING");

        transaction.setIdempotencyKey(
                "IDEMP-LEDGER-" + UUID.randomUUID()
        );

        transaction.setReferenceId(
                "REF-LEDGER-" + UUID.randomUUID()
        );

        transaction.setCreatedAt(
                LocalDateTime.now()
        );


        Transaction savedTransaction =
                transactionService.create(
                        transaction
                );


        List<LedgerEntry> entries =
                ledgerEntryRepository.findByTransactionId(
                        savedTransaction.getId()
                );


        assertEquals(
                2,
                entries.size()
        );


        long debitCount =
                entries.stream()
                        .filter(entry ->
                                entry.getEntryType()
                                        == LedgerEntryType.DEBIT
                        )
                        .count();


        long creditCount =
                entries.stream()
                        .filter(entry ->
                                entry.getEntryType()
                                        == LedgerEntryType.CREDIT
                        )
                        .count();


        assertEquals(
                1,
                debitCount
        );

        assertEquals(
                1,
                creditCount
        );


        BigDecimal debitAmount =
                entries.stream()
                        .filter(entry ->
                                entry.getEntryType()
                                        == LedgerEntryType.DEBIT
                        )
                        .findFirst()
                        .orElseThrow()
                        .getAmount();


        BigDecimal creditAmount =
                entries.stream()
                        .filter(entry ->
                                entry.getEntryType()
                                        == LedgerEntryType.CREDIT
                        )
                        .findFirst()
                        .orElseThrow()
                        .getAmount();


        assertEquals(
                0,
                debitAmount.compareTo(
                        creditAmount
                )
        );
    }


    @Test
    void negativeLedgerAmountShouldBeRejected() {

        Settlement settlement = new Settlement();

        settlement.setId(
                UUID.randomUUID()
        );

        settlement.setMerchantId(
                "MERCHANT-INVALID-" + UUID.randomUUID()
        );

        settlement.setAmount(
                new BigDecimal("100.00")
        );

        settlement.setCreatedAt(
                LocalDateTime.now()
        );

        settlement =
                settlementRepository.saveAndFlush(
                        settlement
                );


        Transaction transaction = new Transaction();

        transaction.setId(
                UUID.randomUUID()
        );

        transaction.setSettlementId(
                settlement.getId()
        );

        transaction.setMerchantId(
                settlement.getMerchantId()
        );

        transaction.setAccountId(
                "ACCOUNT-INVALID-" + UUID.randomUUID()
        );

        transaction.setAmount(
                new BigDecimal("-100.00")
        );

        transaction.setStatus("PENDING");

        transaction.setIdempotencyKey(
                "IDEMP-INVALID-" + UUID.randomUUID()
        );

        transaction.setReferenceId(
                "REF-INVALID-" + UUID.randomUUID()
        );

        transaction.setCreatedAt(
                LocalDateTime.now()
        );


        assertThrows(
                Exception.class,
                () -> transactionService.create(
                        transaction
                )
        );
    }


    @Test
    void invalidLedgerAmountShouldRollbackTransactionAndLedgerEntries() {

        Settlement settlement = new Settlement();

        settlement.setId(
                UUID.randomUUID()
        );

        settlement.setMerchantId(
                "MERCHANT-ROLLBACK-" + UUID.randomUUID()
        );

        settlement.setAmount(
                new BigDecimal("100.00")
        );

        settlement.setCreatedAt(
                LocalDateTime.now()
        );

        settlement =
                settlementRepository.saveAndFlush(
                        settlement
                );


        UUID transactionId =
                UUID.randomUUID();


        Transaction transaction =
                new Transaction();

        transaction.setId(
                transactionId
        );

        transaction.setSettlementId(
                settlement.getId()
        );

        transaction.setMerchantId(
                settlement.getMerchantId()
        );

        transaction.setAccountId(
                "ACCOUNT-ROLLBACK-" + UUID.randomUUID()
        );

        transaction.setAmount(
                new BigDecimal("-100.00")
        );

        transaction.setStatus(
                "PENDING"
        );

        transaction.setIdempotencyKey(
                "IDEMP-ROLLBACK-" + UUID.randomUUID()
        );

        transaction.setReferenceId(
                "REF-ROLLBACK-" + UUID.randomUUID()
        );

        transaction.setCreatedAt(
                LocalDateTime.now()
        );


        assertThrows(
                Exception.class,
                () -> transactionService.create(
                        transaction
                )
        );


        assertTrue(
                transactionRepository
                        .findById(transactionId)
                        .isEmpty()
        );


        assertTrue(
                ledgerEntryRepository
                        .findByTransactionId(transactionId)
                        .isEmpty()
        );
    }


    @Test
    void reconciliationApiShouldReturnMatchedMismatchAndUnmatched()
            throws Exception {

        /*
         * Every execution gets a unique suffix because
         * transactions.reference_id has a UNIQUE constraint.
         */
        String testRunId =
                UUID.randomUUID().toString();

        /*
         * One explicit reconciliation batch.
         *
         * Internal transactions and external records belonging
         * to this reconciliation run must use the same batch ID.
         */
        String batchId =
                "BATCH-RECON-" + testRunId;


        String matchedReference =
                "REF-API-MATCHED-" + testRunId;

        String mismatchReference =
                "REF-API-MISMATCH-" + testRunId;

        String internalOnlyReference =
                "REF-API-INTERNAL-ONLY-" + testRunId;

        String externalOnlyReference =
                "REF-API-EXTERNAL-ONLY-" + testRunId;


        /*
         * ---------------------------------------------------------
         * INTERNAL TRANSACTIONS
         * ---------------------------------------------------------
         */

        Transaction matchedTransaction =
                createReconciliationTransaction(
                        matchedReference,
                        new BigDecimal("100.00"),
                        testRunId,
                        batchId
                );


        Transaction mismatchTransaction =
                createReconciliationTransaction(
                        mismatchReference,
                        new BigDecimal("200.00"),
                        testRunId,
                        batchId
                );


        Transaction internalOnlyTransaction =
                createReconciliationTransaction(
                        internalOnlyReference,
                        new BigDecimal("700.00"),
                        testRunId,
                        batchId
                );


        /*
         * ---------------------------------------------------------
         * HISTORICAL TRANSACTION
         * ---------------------------------------------------------
         *
         * This transaction belongs to another batch.
         *
         * The reconciliation service MUST NOT load or classify
         * this transaction during the current batch.
         */

        String historicalBatchId =
                "OLD-BATCH-" + testRunId;

        String historicalReference =
                "REF-HISTORICAL-" + testRunId;

        Transaction historicalTransaction =
                createReconciliationTransaction(
                        historicalReference,
                        new BigDecimal("999.00"),
                        testRunId,
                        historicalBatchId
                );


        /*
         * ---------------------------------------------------------
         * EXTERNAL RECORDS
         * ---------------------------------------------------------
         *
         * MATCHED:
         * internal = 100
         * external = 100
         *
         * AMOUNT_MISMATCH:
         * internal = 200
         * external = 150
         *
         * EXTERNAL ONLY:
         * no internal transaction exists
         */

        String requestBody = """
                [
                  {
                    "batchId": "%s",
                    "referenceId": "%s",
                    "amount": 100.00,
                    "timestamp": "2026-09-13T02:00:00"
                  },
                  {
                    "batchId": "%s",
                    "referenceId": "%s",
                    "amount": 150.00,
                    "timestamp": "2026-09-13T02:01:00"
                  },
                  {
                    "batchId": "%s",
                    "referenceId": "%s",
                    "amount": 500.00,
                    "timestamp": "2026-09-13T02:02:00"
                  }
                ]
                """.formatted(
                batchId,
                matchedReference,
                batchId,
                mismatchReference,
                batchId,
                externalOnlyReference
        );


        /*
         * ---------------------------------------------------------
         * CALL RECONCILIATION API
         * ---------------------------------------------------------
         */

        mockMvc.perform(
                        post("/reconciliation")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isOk()
                )


                /*
                 * MATCHED
                 */
                .andExpect(
                        jsonPath(
                                "$[?(@.referenceId == '" +
                                        matchedReference +
                                        "')].status"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "MATCHED"
                                )
                        )
                )


                /*
                 * AMOUNT MISMATCH
                 */
                .andExpect(
                        jsonPath(
                                "$[?(@.referenceId == '" +
                                        mismatchReference +
                                        "')].status"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "AMOUNT_MISMATCH"
                                )
                        )
                )


                /*
                 * INTERNAL ONLY
                 */
                .andExpect(
                        jsonPath(
                                "$[?(@.referenceId == '" +
                                        internalOnlyReference +
                                        "')].status"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "UNMATCHED"
                                )
                        )
                )


                /*
                 * EXTERNAL ONLY
                 */
                .andExpect(
                        jsonPath(
                                "$[?(@.referenceId == '" +
                                        externalOnlyReference +
                                        "')].status"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "UNMATCHED"
                                )
                        )
                );


        /*
         * ---------------------------------------------------------
         * VERIFY MATCHED TRANSACTION WAS PERSISTED
         * ---------------------------------------------------------
         *
         * Before reconciliation:
         *
         *     status = PENDING
         *
         * After reconciliation:
         *
         *     status = MATCHED
         *
         * We reload the transaction from PostgreSQL instead of
         * checking the Java object in memory.
         */

        Transaction persistedMatchedTransaction =
                transactionRepository
                        .findById(
                                matchedTransaction.getId()
                        )
                        .orElseThrow();


        assertEquals(
                "MATCHED",
                persistedMatchedTransaction.getStatus()
        );


        /*
         * ---------------------------------------------------------
         * VERIFY HISTORICAL TRANSACTION WAS NOT TOUCHED
         * ---------------------------------------------------------
         */

        Transaction persistedHistoricalTransaction =
                transactionRepository
                        .findById(
                                historicalTransaction.getId()
                        )
                        .orElseThrow();


        assertEquals(
                "PENDING",
                persistedHistoricalTransaction.getStatus()
        );


        /*
         * There must be no exception for the historical transaction
         * in the current reconciliation batch.
         */

        assertTrue(
                reconciliationExceptionRepository
                        .findAll()
                        .stream()
                        .noneMatch(exception ->
                                batchId.equals(
                                        exception.getBatchId()
                                )
                                        &&
                                historicalReference.equals(
                                        exception.getReferenceId()
                                )
                        )
        );


        /*
         * ---------------------------------------------------------
         * VERIFY AMOUNT MISMATCH EXCEPTION
         * ---------------------------------------------------------
         */

        ReconciliationException mismatchException =
                reconciliationExceptionRepository
                        .findAll()
                        .stream()
                        .filter(exception ->
                                batchId.equals(
                                        exception.getBatchId()
                                )
                                        &&
                                mismatchReference.equals(
                                        exception.getReferenceId()
                                )
                                        &&
                                "AMOUNT_MISMATCH".equals(
                                        exception.getExceptionType()
                                )
                        )
                        .findFirst()
                        .orElseThrow();


        assertEquals(
                "AMOUNT_MISMATCH",
                mismatchException.getExceptionType()
        );


        assertEquals(
                mismatchTransaction.getId(),
                mismatchException.getTransactionId()
        );


        /*
         * BigDecimal.compareTo() is intentionally used here.
         *
         * 200.00 and 200.0000 have different scales but
         * represent the same monetary value.
         */

        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(
                                mismatchException.getInternalAmount()
                        )
        );


        assertEquals(
                0,
                new BigDecimal("150.00")
                        .compareTo(
                                mismatchException.getExternalAmount()
                        )
        );


        /*
         * ---------------------------------------------------------
         * VERIFY INTERNAL-ONLY UNMATCHED EXCEPTION
         * ---------------------------------------------------------
         */

        ReconciliationException internalOnlyException =
                reconciliationExceptionRepository
                        .findAll()
                        .stream()
                        .filter(exception ->
                                batchId.equals(
                                        exception.getBatchId()
                                )
                                        &&
                                internalOnlyReference.equals(
                                        exception.getReferenceId()
                                )
                                        &&
                                "UNMATCHED".equals(
                                        exception.getExceptionType()
                                )
                        )
                        .findFirst()
                        .orElseThrow();


        assertEquals(
                "UNMATCHED",
                internalOnlyException.getExceptionType()
        );


        assertEquals(
                internalOnlyTransaction.getId(),
                internalOnlyException.getTransactionId()
        );


        assertEquals(
                0,
                new BigDecimal("700.00")
                        .compareTo(
                                internalOnlyException.getInternalAmount()
                        )
        );


        assertNull(
                internalOnlyException.getExternalAmount()
        );


        /*
         * ---------------------------------------------------------
         * VERIFY EXTERNAL-ONLY UNMATCHED EXCEPTION
         * ---------------------------------------------------------
         */

        ReconciliationException externalOnlyException =
                reconciliationExceptionRepository
                        .findAll()
                        .stream()
                        .filter(exception ->
                                batchId.equals(
                                        exception.getBatchId()
                                )
                                        &&
                                externalOnlyReference.equals(
                                        exception.getReferenceId()
                                )
                                        &&
                                "UNMATCHED".equals(
                                        exception.getExceptionType()
                                )
                        )
                        .findFirst()
                        .orElseThrow();


        assertEquals(
                "UNMATCHED",
                externalOnlyException.getExceptionType()
        );


        /*
         * External-only means there is no internal transaction.
         */

        assertNull(
                externalOnlyException.getTransactionId()
        );


        assertNull(
                externalOnlyException.getInternalAmount()
        );


        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(
                                externalOnlyException.getExternalAmount()
                        )
        );


        /*
         * ---------------------------------------------------------
         * VERIFY BATCH ID
         * ---------------------------------------------------------
         */

        assertEquals(
                batchId,
                mismatchException.getBatchId()
        );

        assertEquals(
                batchId,
                internalOnlyException.getBatchId()
        );

        assertEquals(
                batchId,
                externalOnlyException.getBatchId()
        );


        /*
         * ---------------------------------------------------------
         * RETRY / IDEMPOTENCY TEST
         * ---------------------------------------------------------
         *
         * Re-running the exact same reconciliation batch must
         * NOT create duplicate exception rows.
         */

        long exceptionCountAfterFirstRun =
                reconciliationExceptionRepository
                        .findAll()
                        .stream()
                        .filter(exception ->
                                batchId.equals(
                                        exception.getBatchId()
                                )
                        )
                        .count();


        mockMvc.perform(
                        post("/reconciliation")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isOk()
                );


        long exceptionCountAfterSecondRun =
                reconciliationExceptionRepository
                        .findAll()
                        .stream()
                        .filter(exception ->
                                batchId.equals(
                                        exception.getBatchId()
                                )
                        )
                        .count();


        assertEquals(
                exceptionCountAfterFirstRun,
                exceptionCountAfterSecondRun
        );
    }


    /*
     * -------------------------------------------------------------
     * TEST DATA HELPER
     * -------------------------------------------------------------
     */

    private Transaction createReconciliationTransaction(
            String referenceId,
            BigDecimal amount,
            String testRunId,
            String batchId) {

        Settlement settlement =
                new Settlement();


        settlement.setId(
                UUID.randomUUID()
        );


        settlement.setMerchantId(
                "MERCHANT-RECON-" + testRunId
        );


        settlement.setAmount(
                amount
        );


        settlement.setCreatedAt(
                LocalDateTime.now()
        );


        settlement =
                settlementRepository.saveAndFlush(
                        settlement
                );


        Transaction transaction =
                new Transaction();


        transaction.setId(
                UUID.randomUUID()
        );


        transaction.setSettlementId(
                settlement.getId()
        );


        transaction.setMerchantId(
                settlement.getMerchantId()
        );


        transaction.setAccountId(
                "ACCOUNT-RECON-" + UUID.randomUUID()
        );


        transaction.setAmount(
                amount
        );


        transaction.setStatus(
                "PENDING"
        );


        transaction.setIdempotencyKey(
                "IDEMP-RECON-" + UUID.randomUUID()
        );


        transaction.setReferenceId(
                referenceId
        );


        /*
         * Every transaction created for this test explicitly
         * belongs to the reconciliation batch.
         */

        transaction.setReconciliationBatchId(
                batchId
        );


        transaction.setCreatedAt(
                LocalDateTime.now()
        );


        return transactionRepository.saveAndFlush(
                transaction
        );
    }
}
