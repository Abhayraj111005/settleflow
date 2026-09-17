package com.settleflow.reconciliation;

import com.settleflow.entity.Transaction;
import com.settleflow.repository.TransactionRepository;
import com.settleflow.reconciliation.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReconciliationJob {

    private static final Logger log =
            LoggerFactory.getLogger(ReconciliationJob.class);

    private final TransactionRepository transactionRepository;
    private final ExternalRecordProvider externalRecordProvider;
    private final ReconciliationService reconciliationService;

    private LocalDateTime lastRun = LocalDateTime.MIN;

    public ReconciliationJob(
            TransactionRepository transactionRepository,
            ExternalRecordProvider externalRecordProvider,
            ReconciliationService reconciliationService) {

        this.transactionRepository = transactionRepository;
        this.externalRecordProvider = externalRecordProvider;
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(cron = "${settleflow.reconciliation.schedule}")
    public void runReconciliation() {

        LocalDateTime runStartedAt = LocalDateTime.now();

        log.info(
                "Starting scheduled reconciliation. lastRun={}",
                lastRun
        );

        List<Transaction> unreconciledTransactions =
                transactionRepository.findByReconciliationBatchIdIsNull();

        if (unreconciledTransactions.isEmpty()) {
            log.info(
                    "Scheduled reconciliation skipped: no unreconciled transactions"
            );

            lastRun = runStartedAt;
            return;
        }

        List<ExternalRecord> externalRecords =
                externalRecordProvider.getNewRecordsSince(lastRun);

        if (externalRecords.isEmpty()) {
            log.info(
                    "Scheduled reconciliation skipped: no new external records. " +
                    "internalCandidates={}",
                    unreconciledTransactions.size()
            );

            lastRun = runStartedAt;
            return;
        }

        Set<String> externalReferenceIds =
                externalRecords.stream()
                        .map(ExternalRecord::getReferenceId)
                        .collect(Collectors.toSet());

        List<Transaction> reconciliationCandidates =
                unreconciledTransactions.stream()
                        .filter(transaction ->
                                externalReferenceIds.contains(
                                        transaction.getReferenceId()
                                ))
                        .toList();

        if (reconciliationCandidates.isEmpty()) {
            log.info(
                    "Scheduled reconciliation skipped: no matching internal " +
                    "transactions for new external records. externalRecords={}",
                    externalRecords.size()
            );

            lastRun = runStartedAt;
            return;
        }

        String batchId = "SCHEDULED-" + UUID.randomUUID();

        externalRecords.forEach(record ->
                record.setBatchId(batchId)
        );

        ReconciliationResponse response =
                reconciliationService.reconcile(
                        batchId,
                        reconciliationCandidates,
                        externalRecords
                );

        ReconciliationSummary summary =
                response.getSummary();

        log.info(
                "Scheduled reconciliation completed. batchId={}, summary={}",
                batchId,
                summary
        );

        lastRun = runStartedAt;
    }
}
