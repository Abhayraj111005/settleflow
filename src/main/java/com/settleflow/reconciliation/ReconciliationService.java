package com.settleflow.reconciliation;

import com.settleflow.entity.ReconciliationException;
import com.settleflow.entity.Transaction;
import com.settleflow.repository.ReconciliationExceptionRepository;
import com.settleflow.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationExceptionRepository reconciliationExceptionRepository;
    private final ReconciliationMatcher reconciliationMatcher;
    private final ReconciliationSummaryCalculator reconciliationSummaryCalculator;

    public ReconciliationService(
            TransactionRepository transactionRepository,
            ReconciliationExceptionRepository reconciliationExceptionRepository,
            ReconciliationMatcher reconciliationMatcher,
            ReconciliationSummaryCalculator reconciliationSummaryCalculator) {

        this.transactionRepository = transactionRepository;
        this.reconciliationExceptionRepository =
                reconciliationExceptionRepository;
        this.reconciliationMatcher = reconciliationMatcher;
        this.reconciliationSummaryCalculator =
                reconciliationSummaryCalculator;
    }

    @Transactional
    public ReconciliationResponse reconcile(
            List<ExternalRecord> externalRecords) {

        validateBatch(externalRecords);

        String batchId =
                externalRecords.get(0).getBatchId();

        List<Transaction> internalTransactions =
                transactionRepository.findByReconciliationBatchId(batchId);

        return reconcile(
                batchId,
                internalTransactions,
                externalRecords
        );
    }

    @Transactional
    public ReconciliationResponse reconcile(
            String batchId,
            List<Transaction> internalTransactions,
            List<ExternalRecord> externalRecords) {

        validateInputs(
                batchId,
                internalTransactions,
                externalRecords
        );

        List<ReconciliationResult> results =
                reconciliationMatcher.reconcile(
                        internalTransactions,
                        externalRecords
                );

        ReconciliationSummary summary =
                reconciliationSummaryCalculator.calculate(results);

        for (ReconciliationResult result : results) {

            Transaction internalTransaction =
                    result.getInternalTransaction();

            if (internalTransaction != null) {

                internalTransaction.setReconciliationBatchId(batchId);
            }

            if (result.getStatus() == ReconciliationStatus.MATCHED
                    || result.getStatus()
                    == ReconciliationStatus.PARTIAL_MATCH_RESOLVED) {

                markMatched(result);

            } else if (
                    result.getStatus()
                            == ReconciliationStatus.AMOUNT_MISMATCH
                            || result.getStatus()
                            == ReconciliationStatus.UNMATCHED) {

                persistExceptionIfNeeded(
                        batchId,
                        result
                );
            }

            if (internalTransaction != null) {
                transactionRepository.save(internalTransaction);
            }
        }

        return new ReconciliationResponse(
                summary,
                results
        );
    }

    private void markMatched(
            ReconciliationResult result) {

        Transaction transaction =
                result.getInternalTransaction();

        transaction.setStatus("MATCHED");
    }

    private void persistExceptionIfNeeded(
            String batchId,
            ReconciliationResult result) {

        String referenceId =
                result.getReferenceId();

        String exceptionType =
                result.getStatus().name();

        boolean alreadyExists =
                reconciliationExceptionRepository
                        .findByBatchIdAndReferenceIdAndExceptionType(
                                batchId,
                                referenceId,
                                exceptionType
                        )
                        .isPresent();

        if (alreadyExists) {
            return;
        }

        ReconciliationException exception =
                new ReconciliationException();

        exception.setBatchId(batchId);
        exception.setReferenceId(referenceId);
        exception.setExceptionType(exceptionType);

        Transaction internalTransaction =
                result.getInternalTransaction();

        if (internalTransaction != null) {

            exception.setTransactionId(
                    internalTransaction.getId()
            );

            exception.setInternalAmount(
                    internalTransaction.getAmount()
            );
        }

        ExternalRecord externalRecord =
                result.getExternalRecord();

        if (externalRecord != null) {

            exception.setExternalAmount(
                    externalRecord.getAmount()
            );
        }

        exception.setCreatedAt(
                LocalDateTime.now()
        );

        reconciliationExceptionRepository.save(
                exception
        );
    }

    private void validateBatch(
            List<ExternalRecord> externalRecords) {

        if (externalRecords == null
                || externalRecords.isEmpty()) {

            throw new IllegalArgumentException(
                    "External reconciliation batch cannot be null or empty"
            );
        }

        String batchId =
                externalRecords.get(0).getBatchId();

        if (batchId == null
                || batchId.isBlank()) {

            throw new IllegalArgumentException(
                    "Batch ID cannot be null or blank"
            );
        }

        boolean containsDifferentBatch =
                externalRecords.stream()
                        .anyMatch(record ->
                                record.getBatchId() == null
                                        || !batchId.equals(
                                        record.getBatchId()
                                )
                        );

        if (containsDifferentBatch) {

            throw new IllegalArgumentException(
                    "All external records must belong to the same batch"
            );
        }
    }

    private void validateInputs(
            String batchId,
            List<Transaction> internalTransactions,
            List<ExternalRecord> externalRecords) {

        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException(
                    "Batch ID cannot be null or blank"
            );
        }

        if (internalTransactions == null) {
            throw new IllegalArgumentException(
                    "Internal transactions cannot be null"
            );
        }

        validateBatch(externalRecords);
    }
}
