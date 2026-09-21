package com.settleflow.settleflow;

import com.settleflow.entity.Transaction;
import com.settleflow.entity.ReconciliationException;
import com.settleflow.reconciliation.ReconciliationExceptionStatus;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.settleflow.reconciliation.ExternalRecord;
import com.settleflow.reconciliation.ReconciliationMatcher;
import com.settleflow.reconciliation.ReconciliationService;
import com.settleflow.reconciliation.ReconciliationStatus;
import com.settleflow.reconciliation.ReconciliationSummaryCalculator;
import com.settleflow.reconciliation.ReconciliationResponse;
import com.settleflow.repository.ReconciliationExceptionRepository;
import com.settleflow.repository.TransactionRepository;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReconciliationServiceTest {

    @Test
    void shouldMarkTransactionMatchedWhenPartialMatchIsResolved() {

        Transaction transaction =
                createTransaction(
                        "REF-PARTIAL",
                        "100.00"
                );

        ExternalRecord firstExternalRecord =
                createExternalRecord(
                        "BATCH-001",
                        "REF-PARTIAL",
                        "60.00"
                );

        ExternalRecord secondExternalRecord =
                createExternalRecord(
                        "BATCH-001",
                        "REF-PARTIAL",
                        "40.00"
                );

        TransactionRepository transactionRepository =
                org.mockito.Mockito.mock(TransactionRepository.class);

        ReconciliationExceptionRepository exceptionRepository =
                org.mockito.Mockito.mock(
                        ReconciliationExceptionRepository.class
                );

        when(
                transactionRepository
                        .findByReconciliationBatchId("BATCH-001")
        ).thenReturn(
                List.of(transaction)
        );

        when(
                exceptionRepository
                        .findByBatchIdAndReferenceIdAndExceptionType(
                                any(),
                                any(),
                                any()
                        )
        ).thenReturn(Optional.empty());

        ReconciliationMatcher matcher =
                new ReconciliationMatcher();

        ReconciliationSummaryCalculator summaryCalculator =
                new ReconciliationSummaryCalculator();

        ReconciliationService service =
                new ReconciliationService(
                        transactionRepository,
                        exceptionRepository,
                        matcher,
                        summaryCalculator
                );

        ReconciliationResponse response =
                service.reconcile(
                        List.of(
                                firstExternalRecord,
                                secondExternalRecord
                        )
                );

        assertEquals(
                "MATCHED",
                transaction.getStatus()
        );

        assertEquals(
                ReconciliationStatus.PARTIAL_MATCH_RESOLVED,
                response.getResults()
                        .get(0)
                        .getStatus()
        );

        verify(transactionRepository)
                .save(transaction);
    }

@Test
void shouldResolveOpenExceptionManually() {

    UUID exceptionId = UUID.randomUUID();

    ReconciliationException exception =
            new ReconciliationException();

    exception.setId(exceptionId);
    exception.setBatchId("BATCH-001");
    exception.setReferenceId("REF-001");
    exception.setExceptionType("AMOUNT_MISMATCH");
    exception.setStatus(
            ReconciliationExceptionStatus.OPEN
    );
    exception.setCreatedAt(
            LocalDateTime.now()
    );

    ReconciliationExceptionRepository exceptionRepository =
            org.mockito.Mockito.mock(
                    ReconciliationExceptionRepository.class
            );

    when(
            exceptionRepository.findById(exceptionId)
    ).thenReturn(
            Optional.of(exception)
    );

    when(
            exceptionRepository.save(exception)
    ).thenReturn(exception);

    TransactionRepository transactionRepository =
            org.mockito.Mockito.mock(
                    TransactionRepository.class
            );

    ReconciliationService service =
            new ReconciliationService(
                    transactionRepository,
                    exceptionRepository,
                    new ReconciliationMatcher(),
                    new ReconciliationSummaryCalculator()
            );

    ReconciliationException resolved =
            service.resolveException(
                    exceptionId,
                    "  Verified against bank statement  "
            );

    assertEquals(
            ReconciliationExceptionStatus.RESOLVED_MANUALLY,
            resolved.getStatus()
    );

    assertEquals(
            "Verified against bank statement",
            resolved.getResolutionNote()
    );

    assertNotNull(
            resolved.getResolvedAt()
    );

    verify(exceptionRepository)
            .save(exception);
}

    private Transaction createTransaction(
            String referenceId,
            String amount) {

        Transaction transaction =
                new Transaction();

        transaction.setId(UUID.randomUUID());
        transaction.setReferenceId(referenceId);
        transaction.setMerchantId("MERCHANT-001");
        transaction.setAccountId("ACCOUNT-001");
        transaction.setAmount(new BigDecimal(amount));
        transaction.setStatus("PENDING");

        transaction.setIdempotencyKey(
                "service-test-" + UUID.randomUUID()
        );

        transaction.setCreatedAt(
                LocalDateTime.now()
        );

        return transaction;
    }

    private ExternalRecord createExternalRecord(
            String batchId,
            String referenceId,
            String amount) {

        return new ExternalRecord(
                batchId,
                referenceId,
                new BigDecimal(amount),
                LocalDateTime.now()
        );
    }

@Test
void shouldRejectResolvingAlreadyResolvedException() {

    UUID exceptionId = UUID.randomUUID();

    ReconciliationException exception =
            new ReconciliationException();

    exception.setId(exceptionId);
    exception.setBatchId("BATCH-001");
    exception.setReferenceId("REF-001");
    exception.setExceptionType("AMOUNT_MISMATCH");
    exception.setStatus(
            ReconciliationExceptionStatus.RESOLVED_MANUALLY
    );
    exception.setCreatedAt(LocalDateTime.now());

    ReconciliationExceptionRepository exceptionRepository =
            org.mockito.Mockito.mock(
                    ReconciliationExceptionRepository.class
            );

    when(
            exceptionRepository.findById(exceptionId)
    ).thenReturn(
            Optional.of(exception)
    );

    ReconciliationService service =
            new ReconciliationService(
                    org.mockito.Mockito.mock(TransactionRepository.class),
                    exceptionRepository,
                    new ReconciliationMatcher(),
                    new ReconciliationSummaryCalculator()
            );

    assertThrows(
            IllegalStateException.class,
            () -> service.resolveException(
                    exceptionId,
                    "Trying to resolve again"
            )
    );
}

@Test
void shouldRejectBlankResolutionNote() {

    UUID exceptionId = UUID.randomUUID();

    ReconciliationException exception =
            new ReconciliationException();

    exception.setId(exceptionId);
    exception.setBatchId("BATCH-001");
    exception.setReferenceId("REF-001");
    exception.setExceptionType("UNMATCHED");
    exception.setStatus(
            ReconciliationExceptionStatus.OPEN
    );
    exception.setCreatedAt(LocalDateTime.now());

    ReconciliationExceptionRepository exceptionRepository =
            org.mockito.Mockito.mock(
                    ReconciliationExceptionRepository.class
            );

    when(
            exceptionRepository.findById(exceptionId)
    ).thenReturn(
            Optional.of(exception)
    );

    ReconciliationService service =
            new ReconciliationService(
                    org.mockito.Mockito.mock(TransactionRepository.class),
                    exceptionRepository,
                    new ReconciliationMatcher(),
                    new ReconciliationSummaryCalculator()
            );

    assertThrows(
            IllegalArgumentException.class,
            () -> service.resolveException(
                    exceptionId,
                    "   "
            )
    );
}
@Test
void shouldRejectUnknownExceptionId() {

    UUID exceptionId = UUID.randomUUID();

    ReconciliationExceptionRepository exceptionRepository =
            org.mockito.Mockito.mock(
                    ReconciliationExceptionRepository.class
            );

    when(
            exceptionRepository.findById(exceptionId)
    ).thenReturn(
            Optional.empty()
    );

    ReconciliationService service =
            new ReconciliationService(
                    org.mockito.Mockito.mock(TransactionRepository.class),
                    exceptionRepository,
                    new ReconciliationMatcher(),
                    new ReconciliationSummaryCalculator()
            );

    assertThrows(
            IllegalArgumentException.class,
            () -> service.resolveException(
                    exceptionId,
                    "Verified manually"
            )
    );
}
}
