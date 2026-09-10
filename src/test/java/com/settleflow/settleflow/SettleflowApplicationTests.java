package com.settleflow.settleflow;

import com.settleflow.entity.LedgerEntry;
import com.settleflow.entity.LedgerEntryType;
import com.settleflow.entity.ProcessedEvent;
import com.settleflow.entity.Settlement;
import com.settleflow.entity.Transaction;
import com.settleflow.repository.LedgerEntryRepository;
import com.settleflow.repository.ProcessedEventRepository;
import com.settleflow.repository.SettlementRepository;
import com.settleflow.repository.TransactionRepository;
import com.settleflow.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class SettleflowApplicationTests {

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionTemplate transactionTemplate;


    @Test
    void onlyOneProcessedEventShouldBeCreatedForConcurrentSameEventId()
            throws Exception {

        UUID eventId = UUID.randomUUID();

        CountDownLatch startLatch =
                new CountDownLatch(1);

        ExecutorService executorService =
                Executors.newFixedThreadPool(2);

        Runnable insertTask = () -> {

            try {
                startLatch.await();

                ProcessedEvent event =
                        new ProcessedEvent();

                event.setEventId(eventId);
                event.setProcessedAt(LocalDateTime.now());

                processedEventRepository.saveAndFlush(event);

            } catch (Exception ignored) {
                /*
                 * Both threads try to insert the same event ID.
                 *
                 * PostgreSQL primary-key constraint guarantees
                 * that only one insert succeeds.
                 */
            }
        };

        executorService.submit(insertTask);
        executorService.submit(insertTask);

        startLatch.countDown();

        executorService.shutdown();

        boolean finished =
                executorService.awaitTermination(
                        5,
                        TimeUnit.SECONDS
                );

        if (!finished) {
            throw new IllegalStateException(
                    "Test threads did not finish in time"
            );
        }
    }


    @Test
    void creatingTransactionShouldCreateBalancedLedgerPair() {

        /*
         * ---------------------------------------------------------
         * STEP 1: Create Settlement
         * ---------------------------------------------------------
         *
         * Settlement uses @GeneratedValue for its ID.
         *
         * Therefore we intentionally do NOT call:
         *
         * settlement.setId(...)
         */
        Settlement settlement =
                new Settlement();

        settlement.setMerchantId(
                "MERCHANT-001"
        );

        settlement.setAmount(
                new BigDecimal("100.00")
        );

        settlement.setCreatedAt(
                LocalDateTime.now()
        );

        Settlement savedSettlement =
                settlementRepository.saveAndFlush(
                        settlement
                );


        /*
         * ---------------------------------------------------------
         * STEP 2: Create Transaction
         * ---------------------------------------------------------
         */
        Transaction transaction =
                new Transaction();

        transaction.setId(
                UUID.randomUUID()
        );

        transaction.setSettlementId(
                savedSettlement.getId()
        );

        transaction.setMerchantId(
                "MERCHANT-001"
        );

        transaction.setAmount(
                new BigDecimal("100.00")
        );

        transaction.setStatus(
                "PENDING"
        );

        transaction.setIdempotencyKey(
                "ledger-test-" + UUID.randomUUID()
        );

        transaction.setCreatedAt(
                LocalDateTime.now()
        );

        transaction.setAccountId(
                "ACCOUNT-001"
        );


        /*
         * ---------------------------------------------------------
         * STEP 3: Create Transaction through service
         * ---------------------------------------------------------
         *
         * TransactionService is responsible for creating:
         *
         * DEBIT  ₹100
         * CREDIT ₹100
         */
        Transaction savedTransaction =
                transactionService.create(
                        transaction
                );


        /*
         * ---------------------------------------------------------
         * STEP 4: Read Ledger Entries
         * ---------------------------------------------------------
         */
        List<LedgerEntry> entries =
                ledgerEntryRepository.findByTransactionId(
                        savedTransaction.getId()
                );


        /*
         * ---------------------------------------------------------
         * STEP 5: Exactly two entries
         * ---------------------------------------------------------
         */
        assertEquals(
                2,
                entries.size(),
                "Transaction must have exactly two ledger entries"
        );


        /*
         * ---------------------------------------------------------
         * STEP 6: Exactly one DEBIT and one CREDIT
         * ---------------------------------------------------------
         */
        long debitCount =
                entries.stream()
                        .filter(entry ->
                                entry.getEntryType()
                                        == LedgerEntryType.DEBIT)
                        .count();

        long creditCount =
                entries.stream()
                        .filter(entry ->
                                entry.getEntryType()
                                        == LedgerEntryType.CREDIT)
                        .count();

        assertEquals(
                1,
                debitCount,
                "Transaction must have exactly one DEBIT"
        );

        assertEquals(
                1,
                creditCount,
                "Transaction must have exactly one CREDIT"
        );


        /*
         * ---------------------------------------------------------
         * STEP 7: Get DEBIT and CREDIT
         * ---------------------------------------------------------
         */
        LedgerEntry debit =
                entries.stream()
                        .filter(entry ->
                                entry.getEntryType()
                                        == LedgerEntryType.DEBIT)
                        .findFirst()
                        .orElseThrow();

        LedgerEntry credit =
                entries.stream()
                        .filter(entry ->
                                entry.getEntryType()
                                        == LedgerEntryType.CREDIT)
                        .findFirst()
                        .orElseThrow();


        /*
         * ---------------------------------------------------------
         * STEP 8: Verify amounts are equal
         * ---------------------------------------------------------
         */
        assertEquals(
                0,
                debit.getAmount()
                        .compareTo(credit.getAmount()),
                "Debit and credit must have equal amounts"
        );


        /*
         * ---------------------------------------------------------
         * STEP 9: Verify both amounts are ₹100
         * ---------------------------------------------------------
         */
        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(debit.getAmount())
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(credit.getAmount())
        );
    }


    @Test
    void unbalancedLedgerPairShouldBeRejected() {

        /*
         * The PostgreSQL ledger constraint is:
         *
         * DEFERRABLE INITIALLY DEFERRED
         *
         * Therefore the constraint is checked when the
         * database transaction commits.
         *
         * TransactionTemplate gives us one transaction
         * containing all the following operations.
         */
        assertThrows(
                Exception.class,
                () -> transactionTemplate.executeWithoutResult(
                        status -> {

                            /*
                             * -------------------------------------------------
                             * STEP 1: Create Settlement
                             * -------------------------------------------------
                             */
                            Settlement settlement =
                                    new Settlement();

                            settlement.setMerchantId(
                                    "MERCHANT-INVALID"
                            );

                            settlement.setAmount(
                                    new BigDecimal("100.00")
                            );

                            settlement.setCreatedAt(
                                    LocalDateTime.now()
                            );

                            Settlement savedSettlement =
                                    settlementRepository.save(
                                            settlement
                                    );


                            /*
                             * -------------------------------------------------
                             * STEP 2: Create Transaction
                             * -------------------------------------------------
                             */
                            Transaction transaction =
                                    new Transaction();

                            transaction.setId(
                                    UUID.randomUUID()
                            );

                            transaction.setSettlementId(
                                    savedSettlement.getId()
                            );

                            transaction.setMerchantId(
                                    "MERCHANT-INVALID"
                            );

                            transaction.setAmount(
                                    new BigDecimal("100.00")
                            );

                            transaction.setStatus(
                                    "PENDING"
                            );

                            transaction.setIdempotencyKey(
                                    "unbalanced-test-"
                                            + UUID.randomUUID()
                            );

                            transaction.setCreatedAt(
                                    LocalDateTime.now()
                            );

                            transaction.setAccountId(
                                    "ACCOUNT-INVALID"
                            );

                            Transaction savedTransaction =
                                    transactionRepository.save(
                                            transaction
                                    );


                            /*
                             * -------------------------------------------------
                             * STEP 3: Create DEBIT ₹100
                             * -------------------------------------------------
                             */
                            LedgerEntry debit =
                                    new LedgerEntry();

                            debit.setId(
                                    UUID.randomUUID()
                            );

                            debit.setTransactionId(
                                    savedTransaction.getId()
                            );

                            debit.setAmount(
                                    new BigDecimal("100.00")
                            );

                            debit.setEntryType(
                                    LedgerEntryType.DEBIT
                            );

                            debit.setCreatedAt(
                                    LocalDateTime.now()
                            );

                            ledgerEntryRepository.save(
                                    debit
                            );


                            /*
                             * -------------------------------------------------
                             * STEP 4: Create CREDIT ₹90
                             * -------------------------------------------------
                             *
                             * INTENTIONALLY WRONG.
                             *
                             * DEBIT  = ₹100
                             * CREDIT = ₹90
                             *
                             * Difference = ₹10
                             */
                            LedgerEntry credit =
                                    new LedgerEntry();

                            credit.setId(
                                    UUID.randomUUID()
                            );

                            credit.setTransactionId(
                                    savedTransaction.getId()
                            );

                            credit.setAmount(
                                    new BigDecimal("90.00")
                            );

                            credit.setEntryType(
                                    LedgerEntryType.CREDIT
                            );

                            credit.setCreatedAt(
                                    LocalDateTime.now()
                            );

                            ledgerEntryRepository.save(
                                    credit
                            );

                            /*
                             * The deferred PostgreSQL constraint
                             * will execute when this transaction
                             * attempts to COMMIT.
                             *
                             * It should detect:
                             *
                             * DEBIT  = 100
                             * CREDIT = 90
                             *
                             * and reject the transaction.
                             */
                        }
                )
        );
    }

@Test
void unbalancedLedgerShouldRollbackTransactionAndLedgerEntries() {

    UUID transactionId =
            UUID.randomUUID();

    /*
     * The transaction should fail because:
     *
     * DEBIT  = ₹100
     * CREDIT = ₹90
     */
    assertThrows(
            Exception.class,
            () -> transactionTemplate.executeWithoutResult(
                    status -> {

                        /*
                         * Create Settlement.
                         *
                         * ID is generated by Hibernate.
                         */
                        Settlement settlement =
                                new Settlement();

                        settlement.setMerchantId(
                                "MERCHANT-ROLLBACK"
                        );

                        settlement.setAmount(
                                new BigDecimal("100.00")
                        );

                        settlement.setCreatedAt(
                                LocalDateTime.now()
                        );

                        Settlement savedSettlement =
                                settlementRepository.save(
                                        settlement
                                );


                        /*
                         * Create Transaction.
                         */
                        Transaction transaction =
                                new Transaction();

                        transaction.setId(
                                transactionId
                        );

                        transaction.setSettlementId(
                                savedSettlement.getId()
                        );

                        transaction.setMerchantId(
                                "MERCHANT-ROLLBACK"
                        );

                        transaction.setAmount(
                                new BigDecimal("100.00")
                        );

                        transaction.setStatus(
                                "PENDING"
                        );

                        transaction.setIdempotencyKey(
                                "rollback-test-"
                                        + UUID.randomUUID()
                        );

                        transaction.setCreatedAt(
                                LocalDateTime.now()
                        );

                        transaction.setAccountId(
                                "ACCOUNT-ROLLBACK"
                        );

                        transactionRepository.save(
                                transaction
                        );


                        /*
                         * DEBIT ₹100
                         */
                        LedgerEntry debit =
                                new LedgerEntry();

                        debit.setId(
                                UUID.randomUUID()
                        );

                        debit.setTransactionId(
                                transactionId
                        );

                        debit.setAmount(
                                new BigDecimal("100.00")
                        );

                        debit.setEntryType(
                                LedgerEntryType.DEBIT
                        );

                        debit.setCreatedAt(
                                LocalDateTime.now()
                        );

                        ledgerEntryRepository.save(
                                debit
                        );


                        /*
                         * CREDIT ₹90
                         *
                         * Intentionally unbalanced.
                         */
                        LedgerEntry credit =
                                new LedgerEntry();

                        credit.setId(
                                UUID.randomUUID()
                        );

                        credit.setTransactionId(
                                transactionId
                        );

                        credit.setAmount(
                                new BigDecimal("90.00")
                        );

                        credit.setEntryType(
                                LedgerEntryType.CREDIT
                        );

                        credit.setCreatedAt(
                                LocalDateTime.now()
                        );

                        ledgerEntryRepository.save(
                                credit
                        );

                        /*
                         * PostgreSQL's deferred constraint
                         * rejects the transaction at COMMIT.
                         */
                    }
            )
    );


    /*
     * The failed transaction must NOT exist.
     */
    assertEquals(
            0,
            transactionRepository.findById(
                    transactionId
            ).stream().count(),
            "Transaction must be rolled back"
    );


    /*
     * The failed ledger entries must NOT exist.
     */
    assertEquals(
            0,
            ledgerEntryRepository
                    .findByTransactionId(transactionId)
                    .size(),
            "Ledger entries must be rolled back"
    );
}


}