package com.settleflow.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID eventId,
        UUID transactionId,
        UUID settlementId,
        String accountId,
        String merchantId,
        BigDecimal amount,
        String status,
        String idempotencyKey,
        LocalDateTime createdAt
) {
}