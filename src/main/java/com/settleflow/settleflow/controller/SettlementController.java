package com.settleflow.settleflow.controller;

import com.settleflow.settleflow.entity.Settlement;
import com.settleflow.settleflow.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<Settlement> createSettlement(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam String merchantId,
            @RequestParam BigDecimal amount,
            @RequestParam String orderRef) {

        Settlement settlement = settlementService.createSettlement(
                merchantId,
                amount,
                orderRef,
                idempotencyKey
        );

        return ResponseEntity.ok(settlement);
    }
}