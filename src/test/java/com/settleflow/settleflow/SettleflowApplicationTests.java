package com.settleflow.settleflow;

import com.settleflow.entity.LedgerEntry;
import com.settleflow.entity.LedgerEntryType;
import com.settleflow.entity.ReconciliationException;
import com.settleflow.entity.Settlement;
import com.settleflow.entity.Transaction;
import com.settleflow.reconciliation.ReconciliationExceptionStatus;
import com.settleflow.repository.LedgerEntryRepository;
import com.settleflow.repository.ReconciliationExceptionRepository;
import com.settleflow.repository.SettlementRepository;
import com.settleflow.repository.TransactionRepository;
import com.settleflow.service.TransactionService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @PersistenceContext
    private EntityManager entityManager;


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
    void resolvingReconciliationExceptionShouldUpdateDatabase()
            throws Exception {

        ReconciliationException exception =
                new ReconciliationException();

        exception.setBatchId(
                "BATCH-MANUAL-" + UUID.randomUUID()
        );

        exception.setReferenceId(
                "REF-MANUAL-" + UUID.randomUUID()
        );

        exception.setExceptionType(
                "AMOUNT_MISMATCH"
        );

        exception.setStatus(
                ReconciliationExceptionStatus.OPEN
        );

        exception.setCreatedAt(
                LocalDateTime.now()
        );

        exception =
                reconciliationExceptionRepository.saveAndFlush(
                        exception
                );

        String note =
                "Verified against bank statement";

        mockMvc.perform(
                        post(
                                "/reconciliation/exceptions/{exceptionId}/resolve",
                                exception.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "note": "Verified against bank statement"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("RESOLVED_MANUALLY")
                )
                .andExpect(
                        jsonPath("$.resolutionNote")
                                .value(note)
                );

        ReconciliationException resolved =
                reconciliationExceptionRepository
                        .findById(exception.getId())
                        .orElseThrow();

        assertEquals(
                ReconciliationExceptionStatus.RESOLVED_MANUALLY,
                resolved.getStatus()
        );

        assertEquals(
                note,
                resolved.getResolutionNote()
        );

        assertNotNull(
                resolved.getResolvedAt()
        );
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

        String testRunId =
                UUID.randomUUID().toString();


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
                .andExpect(
                        jsonPath("$.summary.totalMatched")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.summary.totalMismatched")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.summary.totalUnmatched")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.summary.totalValueReconciled")
                                .value(100.00)
                )
                .andExpect(
                        jsonPath(
                                "$.results[?(@.referenceId == '" +
                                        matchedReference +
                                        "')].status"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "MATCHED"
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.results[?(@.referenceId == '" +
                                        mismatchReference +
                                        "')].status"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "AMOUNT_MISMATCH"
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.results[?(@.referenceId == '" +
                                        internalOnlyReference +
                                        "')].status"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "UNMATCHED"
                                )
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.results[?(@.referenceId == '" +
                                        externalOnlyReference +
                                        "')].status"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "UNMATCHED"
                                )
                        )
                );


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


    @Test
    void shouldSearchReconciliationExceptionsWithFilters()
            throws Exception {

        ReconciliationException openException =
                new ReconciliationException();

        openException.setBatchId(
                "BATCH-SEARCH-" + UUID.randomUUID()
        );

        openException.setReferenceId(
                "REF-OPEN-" + UUID.randomUUID()
        );

        openException.setExceptionType(
                "AMOUNT_MISMATCH"
        );

        openException.setInternalAmount(
                new BigDecimal("1000.00")
        );

        openException.setExternalAmount(
                new BigDecimal("999.00")
        );

        openException.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        20,
                        10,
                        0
                )
        );

        openException.setStatus(
                ReconciliationExceptionStatus.OPEN
        );


        entityManager.persist(
                openException
        );

        entityManager.flush();


        ReconciliationException resolvedException =
                new ReconciliationException();

        resolvedException.setBatchId(
                "BATCH-SEARCH-" + UUID.randomUUID()
        );

        resolvedException.setReferenceId(
                "REF-RESOLVED-" + UUID.randomUUID()
        );

        resolvedException.setExceptionType(
                "UNMATCHED"
        );

        resolvedException.setInternalAmount(
                new BigDecimal("2000.00")
        );

        resolvedException.setExternalAmount(
                new BigDecimal("2000.00")
        );

        resolvedException.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        19,
                        10,
                        0
                )
        );

        resolvedException.setStatus(
                ReconciliationExceptionStatus.RESOLVED_MANUALLY
        );

        resolvedException.setResolutionNote(
                "Verified manually"
        );

        resolvedException.setResolvedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        20,
                        12,
                        0
                )
        );


        entityManager.persist(
                resolvedException
        );

        entityManager.flush();

        entityManager.clear();


        mockMvc.perform(
                        get("/reconciliation/exceptions")
                                .param(
                                        "status",
                                        "OPEN"
                                )
                                .param(
                                        "amount",
                                        "1000.00"
                                )
                                .param(
                                        "from",
                                        "2026-09-20"
                                )
                                .param(
                                        "to",
                                        "2026-09-20"
                                )
                                .param(
                                        "page",
                                        "0"
                                )
                                .param(
                                        "size",
                                        "20"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].referenceId"
                        ).value(
                                openException.getReferenceId()
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].status"
                        ).value("OPEN")
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].internalAmount"
                        ).value(1000.00)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                );
    }


    @Test
    void shouldSearchResolvedReconciliationExceptions()
            throws Exception {

        String batchId =
                "BATCH-RESOLVED-" + UUID.randomUUID();

        String referenceId =
                "REF-RESOLVED-" + UUID.randomUUID();


        ReconciliationException resolvedException =
                new ReconciliationException();


        resolvedException.setBatchId(
                batchId
        );


        resolvedException.setTransactionId(
                UUID.randomUUID()
        );


        resolvedException.setReferenceId(
                referenceId
        );


        resolvedException.setExceptionType(
                "AMOUNT_MISMATCH"
        );


        resolvedException.setInternalAmount(
                new BigDecimal("2000.00")
        );


        resolvedException.setExternalAmount(
                new BigDecimal("1990.00")
        );


        resolvedException.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        19,
                        10,
                        0
                )
        );


        resolvedException.setStatus(
                ReconciliationExceptionStatus.RESOLVED_MANUALLY
        );


        resolvedException.setResolutionNote(
                "Verified externally"
        );


        resolvedException.setResolvedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        19,
                        12,
                        0
                )
        );


        /*
         * IMPORTANT:
         *
         * The entity uses:
         *
         * @GeneratedValue(strategy = GenerationType.UUID)
         *
         * Therefore we deliberately do not call setId().
         *
         * EntityManager.persist() explicitly marks this as a new
         * entity and avoids Spring Data's save()/merge decision.
         */

        entityManager.persist(
                resolvedException
        );

        entityManager.flush();

        entityManager.clear();


        mockMvc.perform(
                        get("/reconciliation/exceptions")
                                .param(
                                        "status",
                                        "RESOLVED_MANUALLY"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].referenceId"
                        ).value(referenceId)
                )
                .andExpect(
                        jsonPath(
                                "$.content[0].status"
                        ).value(
                                "RESOLVED_MANUALLY"
                        )
                );
    }


    @Test
    void shouldRejectInvalidDateRange()
            throws Exception {

        mockMvc.perform(
                        get("/reconciliation/exceptions")
                                .param(
                                        "from",
                                        "2026-09-21"
                                )
                                .param(
                                        "to",
                                        "2026-09-20"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );
    }


    @Test
    void shouldRejectNegativeAmount()
            throws Exception {

        mockMvc.perform(
                        get("/reconciliation/exceptions")
                                .param(
                                        "amount",
                                        "-100.00"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );
    }


    @Test
    void shouldRejectPageSizeGreaterThan100()
            throws Exception {

        mockMvc.perform(
                        get("/reconciliation/exceptions")
                                .param(
                                        "size",
                                        "101"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );
    }
}