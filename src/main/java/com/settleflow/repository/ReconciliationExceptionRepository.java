package com.settleflow.repository;

import com.settleflow.entity.ReconciliationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

import com.settleflow.reconciliation.ReconciliationExceptionStatus;

public interface ReconciliationExceptionRepository
        extends JpaRepository<ReconciliationException, UUID>,
                JpaSpecificationExecutor<ReconciliationException> {

    Optional<ReconciliationException>
    findByBatchIdAndReferenceIdAndExceptionType(
            String batchId,
            String referenceId,
            String exceptionType
    );

        long countByStatus(ReconciliationExceptionStatus status);
}