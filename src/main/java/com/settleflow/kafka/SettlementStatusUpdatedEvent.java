package com.settleflow.kafka;

import com.settleflow.entity.SettlementStatus;

import java.util.UUID;

public class SettlementStatusUpdatedEvent {

    private UUID settlementId;
    private SettlementStatus previousStatus;
    private SettlementStatus newStatus;

    public SettlementStatusUpdatedEvent() {
    }

    public SettlementStatusUpdatedEvent(
            UUID settlementId,
            SettlementStatus previousStatus,
            SettlementStatus newStatus) {

        this.settlementId = settlementId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public SettlementStatus getPreviousStatus() {
        return previousStatus;
    }

    public SettlementStatus getNewStatus() {
        return newStatus;
    }
}