package com.settleflow.settleflow.controller;

import com.settleflow.settleflow.dto.SettlementRequestDTO;
import com.settleflow.settleflow.dto.SettlementResponseDTO;
import com.settleflow.settleflow.service.SettlementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<SettlementResponseDTO> createSettlement(
            @RequestBody SettlementRequestDTO request) {

        try {
            SettlementResponseDTO response =
                    settlementService.createSettlement(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();

        } catch (IllegalStateException e) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }
    }
}