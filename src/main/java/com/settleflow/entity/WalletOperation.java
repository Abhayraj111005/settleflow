package com.settleflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_operations")
public class WalletOperation {

    @Id
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletOperationType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected WalletOperation() {
        // Required by JPA.
    }

    public WalletOperation(
            UUID id,
            UUID walletId,
            WalletOperationType type,
            BigDecimal amount
    ) {
        if (id == null) {
            throw new IllegalArgumentException("Operation id cannot be null");
        }

        if (walletId == null) {
            throw new IllegalArgumentException("Wallet id cannot be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("Operation type cannot be null");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Operation amount must be greater than zero"
            );
        }

        this.id = id;
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public WalletOperationType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
