package com.settleflow.settleflow.dto;

import java.math.BigDecimal;

public class SettlementRequestDTO {

    private String merchantId;
    private BigDecimal amount;
    private String orderRef;
    private String idempotencyKey;

    public SettlementRequestDTO() {
    }

    public SettlementRequestDTO(String merchantId, BigDecimal amount,
                                String orderRef, String idempotencyKey) {
        this.merchantId = merchantId;
        this.amount = amount;
        this.orderRef = orderRef;
        this.idempotencyKey = idempotencyKey;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}