package com.settleflow.entity;

import jakarta.persistence.*;
import com.settleflow.reconciliation.ReconciliationExceptionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "reconciliation_exceptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reconciliation_exception_batch_reference_type",
                        columnNames = {
                                "batch_id",
                                "reference_id",
                                "exception_type"
                        }
                )
        }
)
public class ReconciliationException {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(name = "exception_type", nullable = false)
    private String exceptionType;

    @Column(
            name = "internal_amount",
            precision = 19,
            scale = 4
    )
    private BigDecimal internalAmount;

    @Column(
            name = "external_amount",
            precision = 19,
            scale = 4
    )
    private BigDecimal externalAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReconciliationExceptionStatus status;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;		
    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }


    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }


    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }


    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }


    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }


    public BigDecimal getInternalAmount() {
        return internalAmount;
    }

    public void setInternalAmount(BigDecimal internalAmount) {
        this.internalAmount = internalAmount;
    }


    public BigDecimal getExternalAmount() {
        return externalAmount;
    }

    public void setExternalAmount(BigDecimal externalAmount) {
        this.externalAmount = externalAmount;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
   public ReconciliationExceptionStatus getStatus() {
    return status;
}

public void setStatus(ReconciliationExceptionStatus status) {
    this.status = status;
}

public String getResolutionNote() {
    return resolutionNote;
}

public void setResolutionNote(String resolutionNote) {
    this.resolutionNote = resolutionNote;
}

public LocalDateTime getResolvedAt() {
    return resolvedAt;
}

public void setResolvedAt(LocalDateTime resolvedAt) {
    this.resolvedAt = resolvedAt;
}
}
