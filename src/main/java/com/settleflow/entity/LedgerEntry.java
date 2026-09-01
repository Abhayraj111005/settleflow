package com.settleflow.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerEntry {

    private UUID id;

    private UUID transactionId;

    private BigDecimal amount;

    private String entryType;

    private LocalDateTime createdAt;
}