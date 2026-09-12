package com.settleflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private SettlementStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public SettlementStatus getStatus() {
    return status;
}

public void transitionTo(SettlementStatus newStatus) {

    if (!isValidTransition(this.status, newStatus)) {
        throw new InvalidSettlementStateTransitionException(
                this.status,
                newStatus
        );
    }

    this.status = newStatus;
}

private boolean isValidTransition(
        SettlementStatus current,
        SettlementStatus next) {

    return switch (current) {

        case PENDING ->
                next == SettlementStatus.PROCESSING;

        case PROCESSING ->
                next == SettlementStatus.COMPLETED
                        || next == SettlementStatus.FAILED;

        case COMPLETED, FAILED ->
                false;
    };
}

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public Settlement() {
    this.status = SettlementStatus.PENDING;
}
}