package com.settleflow.reconciliation;

import com.settleflow.entity.Transaction;
import com.settleflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationMatcher reconciliationMatcher;

    public ReconciliationService(
            TransactionRepository transactionRepository,
            ReconciliationMatcher reconciliationMatcher) {

        this.transactionRepository = transactionRepository;
        this.reconciliationMatcher = reconciliationMatcher;
    }

    public List<ReconciliationResult> reconcile(
            List<ExternalRecord> externalRecords) {

        List<Transaction> internalTransactions =
                transactionRepository.findAll();

        return reconciliationMatcher.reconcile(
                internalTransactions,
                externalRecords
        );
    }
}