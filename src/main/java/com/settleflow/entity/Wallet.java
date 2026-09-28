package com.settleflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "held_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal heldAmount;

    protected Wallet() {
        // Required by JPA.
    }

    public Wallet(UUID id, BigDecimal balance) {
        if (id == null) {
            throw new IllegalArgumentException("Wallet id cannot be null");
        }

        if (balance == null || balance.signum() < 0) {
            throw new IllegalArgumentException(
                    "Wallet balance cannot be null or negative"
            );
        }

        this.id = id;
        this.balance = balance;
        this.heldAmount = BigDecimal.ZERO;
    }

    public void hold(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (amount.compareTo(getAvailableBalance()) > 0) {
            throw new IllegalArgumentException(
                    "Hold amount exceeds available balance"
            );
        }

        heldAmount = heldAmount.add(amount);
    }

    public void release(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (amount.compareTo(heldAmount) > 0) {
            throw new IllegalArgumentException(
                    "Release amount exceeds held amount"
            );
        }

        heldAmount = heldAmount.subtract(amount);
    }

    public void commit(BigDecimal amount) {
        validatePositiveAmount(amount);

        if (amount.compareTo(heldAmount) > 0) {
            throw new IllegalArgumentException(
                    "Commit amount exceeds held amount"
            );
        }

        balance = balance.subtract(amount);
        heldAmount = heldAmount.subtract(amount);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getHeldAmount() {
        return heldAmount;
    }

    public BigDecimal getAvailableBalance() {
        return balance.subtract(heldAmount);
    }
}