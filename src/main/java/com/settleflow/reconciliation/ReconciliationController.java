package com.settleflow.reconciliation;

import org.springframework.web.bind.annotation.*;
import com.settleflow.entity.ReconciliationException;

import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final TokenBucketRateLimiter rateLimiter;

    public ReconciliationController(
        ReconciliationService reconciliationService,
        TokenBucketRateLimiter rateLimiter) {

    this.reconciliationService = reconciliationService;
    this.rateLimiter = rateLimiter;
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
        HttpServletRequest request,

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

    String clientId = request.getRemoteAddr();

    if (!rateLimiter.isAllowed(clientId)) {
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded"
        );
    }

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

