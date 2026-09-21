package com.settleflow.reconciliation;

import com.settleflow.entity.ReconciliationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReconciliationExceptionResponse(
        UUID id,
        String batchId,
        UUID transactionId,
        String referenceId,
        String exceptionType,
        BigDecimal internalAmount,
        BigDecimal externalAmount,
        LocalDateTime createdAt,
        ReconciliationExceptionStatus status,
        String resolutionNote,
        LocalDateTime resolvedAt
) {

    public static ReconciliationExceptionResponse from(
            ReconciliationException exception) {

        return new ReconciliationExceptionResponse(
                exception.getId(),
                exception.getBatchId(),
                exception.getTransactionId(),
                exception.getReferenceId(),
                exception.getExceptionType(),
                exception.getInternalAmount(),
                exception.getExternalAmount(),
                exception.getCreatedAt(),
                exception.getStatus(),
                exception.getResolutionNote(),
                exception.getResolvedAt()
        );
    }
}