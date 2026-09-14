package com.settleflow.repository;

import com.settleflow.entity.ReconciliationException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReconciliationExceptionRepository
        extends JpaRepository<ReconciliationException, UUID> {

    Optional<ReconciliationException>
    findByBatchIdAndReferenceIdAndExceptionType(
            String batchId,
            String referenceId,
            String exceptionType
    );
}