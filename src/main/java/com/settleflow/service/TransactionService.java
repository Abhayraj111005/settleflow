package com.settleflow.service;

import com.settleflow.entity.Transaction;
import com.settleflow.kafka.TransactionCreatedProducer;
import com.settleflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionCreatedProducer transactionCreatedProducer;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionCreatedProducer transactionCreatedProducer) {

        this.transactionRepository = transactionRepository;
        this.transactionCreatedProducer = transactionCreatedProducer;
    }

    @Transactional
    public Transaction create(Transaction transaction) {

        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        transactionCreatedProducer.publish(savedTransaction);

        return savedTransaction;
    }
}