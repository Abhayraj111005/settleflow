package com.settleflow.controller;

import com.settleflow.entity.Settlement;
import com.settleflow.entity.SettlementStatus;
import com.settleflow.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(
            SettlementService settlementService) {

        this.settlementService = settlementService;
    }

    // ---------------------------------------------------------
    // Create Settlement
    // ---------------------------------------------------------

    @PostMapping
    public ResponseEntity<Settlement> createSettlement(
            @RequestParam String merchantId,
            @RequestParam BigDecimal amount) {

        Settlement settlement =
                settlementService.createSettlement(
                        merchantId,
                        amount
                );

        return ResponseEntity.ok(settlement);
    }

    // ---------------------------------------------------------
    // Update Settlement Status
    // ---------------------------------------------------------

    @PatchMapping("/{id}/status")
    public ResponseEntity<Settlement> updateStatus(
            @PathVariable UUID id,
            @RequestParam SettlementStatus status) {

        Settlement settlement =
                settlementService.updateStatus(
                        id,
                        status
                );

        return ResponseEntity.ok(settlement);
    }
}