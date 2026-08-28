package com.settleflow.settleflow.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class SettlementResponseDTO {

    private UUID settlementId;
    private String status;
    private LocalDateTime createdAt;

    public SettlementResponseDTO() {
    }

    public SettlementResponseDTO(UUID settlementId, String status,
                                 LocalDateTime createdAt) {
        this.settlementId = settlementId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(UUID settlementId) {
        this.settlementId = settlementId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}