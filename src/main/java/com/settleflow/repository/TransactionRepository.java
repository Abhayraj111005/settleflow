package com.settleflow.repository;

import com.settleflow.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByReferenceIdIn(List<String> referenceIds);

    List<Transaction> findByReconciliationBatchId(String reconciliationBatchId);

    List<Transaction> findByReconciliationBatchIdIsNull();
}
