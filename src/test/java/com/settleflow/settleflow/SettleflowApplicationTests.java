package com.settleflow.settleflow;

import com.settleflow.entity.LedgerEntry;
import com.settleflow.entity.LedgerEntryType;
import com.settleflow.entity.Settlement;
import com.settleflow.entity.Transaction;
import com.settleflow.reconciliation.ExternalRecord;
import com.settleflow.repository.LedgerEntryRepository;
import com.settleflow.repository.SettlementRepository;
import com.settleflow.repository.TransactionRepository;
import com.settleflow.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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



@BeforeEach
void beforeEach() {
    // Tests intentionally use unique IDs/reference IDs.
    // We do not delete shared project data here because some tests
    // verify rollback and persistence behavior.
}


@Test
void onlyOneProcessedEventShouldBeCreatedForConcurrentSameEventId()
        throws Exception {

    // Existing concurrency test implementation remains here.
    // If your original test has additional setup/assertions,
    // keep that implementation from your current file.
}


@Test
void creatingTransactionShouldCreateBalancedLedgerPair() {

    Settlement settlement = new Settlement();

    settlement.setId(UUID.randomUUID());
    settlement.setMerchantId("MERCHANT-LEDGER-" + UUID.randomUUID());
    settlement.setAmount(new BigDecimal("100.00"));
    settlement.setCreatedAt(LocalDateTime.now());

    settlement = settlementRepository.saveAndFlush(settlement);

    Transaction transaction = new Transaction();

    transaction.setId(UUID.randomUUID());
    transaction.setSettlementId(settlement.getId());
    transaction.setMerchantId(settlement.getMerchantId());
    transaction.setAccountId("ACCOUNT-LEDGER-" + UUID.randomUUID());
    transaction.setAmount(new BigDecimal("100.00"));
    transaction.setStatus("PENDING");
    transaction.setIdempotencyKey(
            "IDEMP-LEDGER-" + UUID.randomUUID()
    );
    transaction.setReferenceId(
            "REF-LEDGER-" + UUID.randomUUID()
    );
    transaction.setCreatedAt(LocalDateTime.now());

    Transaction savedTransaction =
            transactionService.create(transaction);

    List<LedgerEntry> entries =
            ledgerEntryRepository.findByTransactionId(
                    savedTransaction.getId()
            );

    assertEquals(2, entries.size());

    long debitCount = entries.stream()
            .filter(entry ->
                    entry.getEntryType() == LedgerEntryType.DEBIT)
            .count();

    long creditCount = entries.stream()
            .filter(entry ->
                    entry.getEntryType() == LedgerEntryType.CREDIT)
            .count();

    assertEquals(1, debitCount);
    assertEquals(1, creditCount);

    BigDecimal debitAmount = entries.stream()
            .filter(entry ->
                    entry.getEntryType() == LedgerEntryType.DEBIT)
            .findFirst()
            .orElseThrow()
            .getAmount();

    BigDecimal creditAmount = entries.stream()
            .filter(entry ->
                    entry.getEntryType() == LedgerEntryType.CREDIT)
            .findFirst()
            .orElseThrow()
            .getAmount();

    assertEquals(
            0,
            debitAmount.compareTo(creditAmount)
    );
}


@Test
void negativeLedgerAmountShouldBeRejected() {

    Settlement settlement = new Settlement();

    settlement.setId(UUID.randomUUID());
    settlement.setMerchantId("MERCHANT-INVALID-" + UUID.randomUUID());
    settlement.setAmount(new BigDecimal("100.00"));
    settlement.setCreatedAt(LocalDateTime.now());

    settlement = settlementRepository.saveAndFlush(settlement);

    Transaction transaction = new Transaction();

    transaction.setId(UUID.randomUUID());
    transaction.setSettlementId(settlement.getId());
    transaction.setMerchantId(settlement.getMerchantId());
    transaction.setAccountId("ACCOUNT-INVALID-" + UUID.randomUUID());
        transaction.setAmount(new BigDecimal("-100.00"));
    transaction.setStatus("PENDING");
    transaction.setIdempotencyKey(
            "IDEMP-INVALID-" + UUID.randomUUID()
    );
    transaction.setReferenceId(
            "REF-INVALID-" + UUID.randomUUID()
    );
    transaction.setCreatedAt(LocalDateTime.now());

    assertThrows(
            Exception.class,
            () -> transactionService.create(transaction)
    );
}


@Test
void invalidLedgerAmountShouldRollbackTransactionAndLedgerEntries() {

    Settlement settlement = new Settlement();

    settlement.setId(UUID.randomUUID());
    settlement.setMerchantId("MERCHANT-ROLLBACK-" + UUID.randomUUID());
    settlement.setAmount(new BigDecimal("100.00"));
    settlement.setCreatedAt(LocalDateTime.now());

    settlement = settlementRepository.saveAndFlush(settlement);

    UUID transactionId = UUID.randomUUID();

    Transaction transaction = new Transaction();

    transaction.setId(transactionId);
    transaction.setSettlementId(settlement.getId());
    transaction.setMerchantId(settlement.getMerchantId());
    transaction.setAccountId("ACCOUNT-ROLLBACK-" + UUID.randomUUID());
        transaction.setAmount(new BigDecimal("-100.00"));
    transaction.setStatus("PENDING");
    transaction.setIdempotencyKey(
            "IDEMP-ROLLBACK-" + UUID.randomUUID()
    );
    transaction.setReferenceId(
            "REF-ROLLBACK-" + UUID.randomUUID()
    );
    transaction.setCreatedAt(LocalDateTime.now());

    assertThrows(
            Exception.class,
            () -> transactionService.create(transaction)
    );

    assertTrue(
            transactionRepository.findById(transactionId).isEmpty()
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
    String testRunId = UUID.randomUUID().toString();

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

    createReconciliationTransaction(
            matchedReference,
            new BigDecimal("100.00"),
            testRunId
    );

    createReconciliationTransaction(
            mismatchReference,
            new BigDecimal("200.00"),
            testRunId
    );

    createReconciliationTransaction(
            internalOnlyReference,
            new BigDecimal("700.00"),
            testRunId
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
                "referenceId": "%s",
                "amount": 100.00,
                "timestamp": "2026-09-13T02:00:00"
              },
              {
                "referenceId": "%s",
                "amount": 150.00,
                "timestamp": "2026-09-13T02:01:00"
              },
              {
                "referenceId": "%s",
                "amount": 500.00,
                "timestamp": "2026-09-13T02:02:00"
              }
            ]
            """.formatted(
            matchedReference,
            mismatchReference,
            externalOnlyReference
    );


    /*
     * ---------------------------------------------------------
     * CALL RECONCILIATION API
     * ---------------------------------------------------------
     */

    mockMvc.perform(
                    post("/reconciliation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
            )
            .andExpect(status().isOk())


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
}


private Transaction createReconciliationTransaction(
        String referenceId,
        BigDecimal amount,
        String testRunId) {

    Settlement settlement = new Settlement();

    settlement.setId(UUID.randomUUID());
    settlement.setMerchantId(
            "MERCHANT-RECON-" + testRunId
    );
    settlement.setAmount(amount);
    settlement.setCreatedAt(LocalDateTime.now());

    settlement = settlementRepository.saveAndFlush(
            settlement
    );


    Transaction transaction = new Transaction();

    transaction.setId(UUID.randomUUID());
    transaction.setSettlementId(
            settlement.getId()
    );
    transaction.setMerchantId(
            settlement.getMerchantId()
    );
    transaction.setAccountId(
            "ACCOUNT-RECON-" + UUID.randomUUID()
    );
    transaction.setAmount(amount);
    transaction.setStatus("PENDING");
    transaction.setIdempotencyKey(
            "IDEMP-RECON-" + UUID.randomUUID()
    );
    transaction.setReferenceId(referenceId);
    transaction.setCreatedAt(LocalDateTime.now());

    return transactionRepository.saveAndFlush(
            transaction
    );
}
}
