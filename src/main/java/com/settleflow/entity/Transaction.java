package com.settleflow.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {

    private UUID id;

    private UUID settlementId;

    private String merchantId;

    private BigDecimal amount;

    private String status;

    private String idempotencyKey;

    private LocalDateTime createdAt;
}