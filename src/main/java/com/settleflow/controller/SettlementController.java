package com.settleflow.controller;

import com.settleflow.entity.Settlement;
import com.settleflow.service.SettlementService;
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
            @RequestParam String merchantId,
            @RequestParam BigDecimal amount) {

        Settlement settlement =
                settlementService.createSettlement(
                        merchantId,
                        amount
                );

        return ResponseEntity.ok(settlement);
    }
}