package com.settleflow.reconciliation;

import org.springframework.web.bind.annotation.*;
import com.settleflow.entity.ReconciliationException;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

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

@GetMapping("/exceptions")
public Page<ReconciliationExceptionResponse> searchExceptions(

        @RequestParam(required = false)
        ReconciliationExceptionStatus status,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,

        @RequestParam(required = false)
        BigDecimal amount,

        @RequestParam(defaultValue = "0")
        int page,

        @RequestParam(defaultValue = "20")
        int size) {

    return reconciliationService.searchExceptions(
            status,
            from,
            to,
            amount,
            page,
            size
    );
}
}
