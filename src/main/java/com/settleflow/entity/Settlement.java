package com.settleflow.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Settlement {

    private UUID id;

    private String merchantId;

    private BigDecimal amount;

    private String status;

    private LocalDateTime createdAt;
}