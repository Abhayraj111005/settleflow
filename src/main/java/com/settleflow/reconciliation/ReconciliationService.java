package com.settleflow.reconciliation;

import com.settleflow.entity.ReconciliationException;
import com.settleflow.entity.Transaction;
import com.settleflow.repository.ReconciliationExceptionRepository;
import com.settleflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationExceptionRepository reconciliationExceptionRepository;
    private final ReconciliationMatcher reconciliationMatcher;

    public ReconciliationService(
            TransactionRepository transactionRepository,
            ReconciliationExceptionRepository reconciliationExceptionRepository,
            ReconciliationMatcher reconciliationMatcher) {

        this.transactionRepository = transactionRepository;
        this.reconciliationExceptionRepository =
                reconciliationExceptionRepository;
        this.reconciliationMatcher = reconciliationMatcher;
    }

    @Transactional
    public List<ReconciliationResult> reconcile(
            List<ExternalRecord> externalRecords) {

        validateBatch(externalRecords);

        String batchId = externalRecords.get(0).getBatchId();

        /*
         * IMPORTANT:
         *
         * We deliberately DO NOT use:
         *
         * transactionRepository.findAll()
         *
         * because that would compare this external batch
         * against every historical transaction.
         */
        List<Transaction> internalTransactions =
                transactionRepository
                        .findByReconciliationBatchId(batchId);

        List<ReconciliationResult> results =
                reconciliationMatcher.reconcile(
                        internalTransactions,
                        externalRecords
                );

        for (ReconciliationResult result : results) {

            if (result.getStatus() ==
                    ReconciliationStatus.MATCHED) {

                markMatched(result);

            } else if (
                    result.getStatus() ==
                            ReconciliationStatus.AMOUNT_MISMATCH
                            ||
                    result.getStatus() ==
                            ReconciliationStatus.UNMATCHED) {

                persistExceptionIfNeeded(
                        batchId,
                        result
                );
            }
        }

        return results;
    }

    private void markMatched(
            ReconciliationResult result) {

        Transaction transaction =
                result.getInternalTransaction();

        /*
         * MATCHED result always has an internal transaction.
         */
        if (transaction == null) {
            throw new IllegalStateException(
                    "MATCHED result must contain an internal transaction"
            );
        }

        transaction.setStatus("MATCHED");

        transactionRepository.save(transaction);
    }

    private void persistExceptionIfNeeded(
            String batchId,
            ReconciliationResult result) {

        String exceptionType =
                result.getStatus().name();

        /*
         * Application-level idempotency check.
         *
         * The database UNIQUE constraint is the final
         * concurrency safety net.
         */
        boolean alreadyExists =
                reconciliationExceptionRepository
                        .findByBatchIdAndReferenceIdAndExceptionType(
                                batchId,
                                result.getReferenceId(),
                                exceptionType
                        )
                        .isPresent();

        if (alreadyExists) {
            return;
        }

        ReconciliationException exception =
                buildException(
                        batchId,
                        result
                );

        reconciliationExceptionRepository.save(
                exception
        );
    }

    private ReconciliationException buildException(
            String batchId,
            ReconciliationResult result) {

        ReconciliationException exception =
                new ReconciliationException();

        exception.setId(UUID.randomUUID());

        exception.setBatchId(batchId);

        exception.setReferenceId(
                result.getReferenceId()
        );

        exception.setExceptionType(
                result.getStatus().name()
        );

        if (result.getInternalTransaction() != null) {

            exception.setTransactionId(
                    result.getInternalTransaction().getId()
            );

            exception.setInternalAmount(
                    result.getInternalTransaction().getAmount()
            );
        }

        if (result.getExternalRecord() != null) {

            exception.setExternalAmount(
                    result.getExternalRecord().getAmount()
            );
        }

        exception.setCreatedAt(
                LocalDateTime.now()
        );

        return exception;
    }

    private void validateBatch(
            List<ExternalRecord> externalRecords) {

        if (externalRecords == null ||
                externalRecords.isEmpty()) {

            throw new IllegalArgumentException(
                    "External reconciliation batch cannot be empty"
            );
        }

        String batchId =
                externalRecords.get(0).getBatchId();

        if (batchId == null ||
                batchId.isBlank()) {

            throw new IllegalArgumentException(
                    "Reconciliation batchId is required"
            );
        }

        /*
         * Every external record in one API request
         * must belong to the same batch.
         */
        boolean containsDifferentBatch =
                externalRecords.stream()
                        .anyMatch(record ->
                                !batchId.equals(
                                        record.getBatchId()
                                )
                        );

        if (containsDifferentBatch) {

            throw new IllegalArgumentException(
                    "All external records must belong to the same reconciliation batch"
            );
        }
    }
}