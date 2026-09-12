package com.settleflow.reconciliation;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(
            ReconciliationService reconciliationService) {

        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    public List<ReconciliationResult> reconcile(
            @RequestBody List<ExternalRecord> externalRecords) {

        return reconciliationService.reconcile(
                externalRecords
        );
    }
}