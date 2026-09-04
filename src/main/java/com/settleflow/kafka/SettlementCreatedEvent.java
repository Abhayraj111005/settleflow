package com.settleflow.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public class SettlementCreatedEvent {

    private UUID settlementId;
    private String merchantId;
    private BigDecimal amount;
    private String orderRef;

    public SettlementCreatedEvent() {
    }

    public SettlementCreatedEvent(
            UUID settlementId,
            String merchantId,
            BigDecimal amount,
            String orderRef) {
        this.settlementId = settlementId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.orderRef = orderRef;
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(UUID settlementId) {
        this.settlementId = settlementId;
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

    public String getOrderRef() {
        return orderRef;
    }

    public void setOrderRef(String orderRef) {
        this.orderRef = orderRef;
    }
}