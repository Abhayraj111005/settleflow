package com.settleflow.reconciliation;

import com.settleflow.entity.ReconciliationException;
import com.settleflow.entity.Transaction;
import com.settleflow.repository.ReconciliationExceptionRepository;
import com.settleflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.settleflow.reconciliation.ReconciliationExceptionStatus;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReconciliationService {

        private static final Logger log =
                        LoggerFactory.getLogger(ReconciliationService.class);

        private static final int DEFAULT_OPEN_EXCEPTION_THRESHOLD = 10;

    private final TransactionRepository transactionRepository;
    private final ReconciliationExceptionRepository reconciliationExceptionRepository;
    private final ReconciliationMatcher reconciliationMatcher;
    private final ReconciliationSummaryCalculator reconciliationSummaryCalculator;

        @Value("${settleflow.reconciliation.open-exception-threshold:10}")
        private int openExceptionThreshold = DEFAULT_OPEN_EXCEPTION_THRESHOLD;

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
	exception.setStatus(ReconciliationExceptionStatus.OPEN);

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

        long openExceptionCountBeforeSave =
                reconciliationExceptionRepository.countByStatus(
                        ReconciliationExceptionStatus.OPEN
                );

        reconciliationExceptionRepository.save(
                exception
        );

        long openExceptionCountAfterSave =
                reconciliationExceptionCount();

        if (openExceptionCountBeforeSave < openExceptionThreshold
                && openExceptionCountAfterSave >= openExceptionThreshold) {
            log.warn(
                    "OPEN_RECONCILIATION_EXCEPTION_THRESHOLD_CROSSED " +
                            "openExceptionCount={} threshold={}",
                    openExceptionCountAfterSave,
                    openExceptionThreshold
            );
        }
    }

    private long reconciliationExceptionCount() {
        return reconciliationExceptionRepository.countByStatus(
                ReconciliationExceptionStatus.OPEN
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
    @Transactional(readOnly = true)
public Page<ReconciliationExceptionResponse> searchExceptions(
        ReconciliationExceptionStatus status,
        LocalDate from,
        LocalDate to,
        BigDecimal amount,
        int page,
        int size) {

    if (page < 0) {
        throw new IllegalArgumentException(
                "Page must be greater than or equal to 0"
        );
    }

    if (size < 1 || size > 100) {
        throw new IllegalArgumentException(
                "Page size must be between 1 and 100"
        );
    }

    if (from != null
            && to != null
            && from.isAfter(to)) {

        throw new IllegalArgumentException(
                "From date cannot be after to date"
        );
    }

    if (amount != null
            && amount.compareTo(BigDecimal.ZERO) < 0) {

        throw new IllegalArgumentException(
                "Amount cannot be negative"
        );
    }

    Specification<ReconciliationException> specification =
            (root, query, criteriaBuilder) ->
                    criteriaBuilder.conjunction();

    if (status != null) {
        specification =
                specification.and(
                        ReconciliationExceptionSpecification
                                .statusEquals(status)
                );
    }

    if (from != null) {
        LocalDateTime fromDateTime =
                from.atStartOfDay();

        specification =
                specification.and(
                        ReconciliationExceptionSpecification
                                .createdAtGreaterThanOrEqual(
                                        fromDateTime
                                )
                );
    }

    if (to != null) {
        LocalDateTime toExclusive =
                to.plusDays(1).atStartOfDay();

        specification =
                specification.and(
                        ReconciliationExceptionSpecification
                                .createdAtLessThan(
                                        toExclusive
                                )
                );
    }

    if (amount != null) {
        specification =
                specification.and(
                        ReconciliationExceptionSpecification
                                .amountEquals(amount)
                );
    }

    Pageable pageable =
            PageRequest.of(
                    page,
                    size
            );

    return reconciliationExceptionRepository
            .findAll(
                    specification,
                    pageable
            )
            .map(ReconciliationExceptionResponse::from);
}

@Transactional
public ReconciliationException resolveException(
        java.util.UUID exceptionId,
        String note) {

    ReconciliationException exception =
            reconciliationExceptionRepository.findById(exceptionId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Reconciliation exception not found: "
                                            + exceptionId
                            )
                    );

    if (exception.getStatus()
            != ReconciliationExceptionStatus.OPEN) {

        throw new IllegalStateException(
                "Reconciliation exception is not open: "
                        + exceptionId
        );
    }

    if (note == null || note.isBlank()) {

        throw new IllegalArgumentException(
                "Resolution note cannot be null or blank"
        );
    }

    exception.setStatus(
            ReconciliationExceptionStatus.RESOLVED_MANUALLY
    );

    exception.setResolutionNote(note.trim());

    exception.setResolvedAt(LocalDateTime.now());

    return reconciliationExceptionRepository.save(exception);
}
}

