package com.settleflow.reconciliation;

import org.springframework.web.bind.annotation.*;
import com.settleflow.entity.ReconciliationException;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;
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
    public ReconciliationResponse reconcile(
            @RequestBody List<ExternalRecord> externalRecords) {

        return reconciliationService.reconcile(externalRecords);
    
}
   @PostMapping("/exceptions/{exceptionId}/resolve")
public ReconciliationException resolveException(
        @PathVariable UUID exceptionId,
        @RequestBody ResolutionRequest request) {

    return reconciliationService.resolveException(
            exceptionId,
            request.note()
    );
}
 public record ResolutionRequest(String note) {
    }
}
