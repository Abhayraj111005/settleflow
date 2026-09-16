package com.settleflow.settleflow;

import com.settleflow.entity.Transaction;
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
}