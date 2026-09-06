package com.settleflow.kafka;

import com.settleflow.entity.ProcessedEvent;
import com.settleflow.entity.Transaction;
import com.settleflow.repository.ProcessedEventRepository;
import com.settleflow.repository.TransactionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionCreatedConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final TransactionRepository transactionRepository;

    public TransactionCreatedConsumer(
            ProcessedEventRepository processedEventRepository,
            TransactionRepository transactionRepository) {

        this.processedEventRepository = processedEventRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    @KafkaListener(
            topics = "settlement.transactions.created",
            groupId = "settlement-transaction-consumer-group",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    public void consume(
            TransactionCreatedEvent event,
            Acknowledgment acknowledgment) {

        System.out.println(
                "Received transaction event: " + event
        );

        System.out.println(
                "Processing event: " + event.eventId()
        );

        // ========================================================
        // Idempotency check
        // ========================================================

        if (processedEventRepository.existsByEventId(event.eventId())) {

            System.out.println(
                    "Duplicate event detected. Skipping: "
                            + event.eventId()
            );

            acknowledgment.acknowledge();

            return;
        }

        // ========================================================
        // Create ProcessedEvent record
        // ========================================================

        ProcessedEvent processedEvent = new ProcessedEvent();

        processedEvent.setEventId(event.eventId());
        processedEvent.setProcessedAt(LocalDateTime.now());

        // ========================================================
        // Persist ProcessedEvent
        // ========================================================

        saveWithRetry(processedEvent);

        // ========================================================
        // Create Transaction
        // ========================================================

        Transaction transaction = new Transaction();

        transaction.setId(event.transactionId());
        transaction.setSettlementId(event.settlementId());
        transaction.setAccountId(event.accountId());
        transaction.setMerchantId(event.merchantId());
        transaction.setAmount(event.amount());
        transaction.setStatus(event.status());
        transaction.setIdempotencyKey(event.idempotencyKey());
        transaction.setCreatedAt(event.createdAt());

        // ========================================================
        // Persist Transaction
        // ========================================================

        transactionRepository.saveAndFlush(transaction);
        System.out.println(
                "Transaction persisted successfully: "
                        + event.transactionId()
        );

        // ========================================================
        // Acknowledge Kafka message
        // ========================================================

        acknowledgment.acknowledge();
    }

    private void saveWithRetry(ProcessedEvent processedEvent) {

        int maxAttempts = 3;
        long backoffMillis = 100;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                processedEventRepository.saveAndFlush(processedEvent);

                return;

            } catch (Exception e) {

                if (attempt == maxAttempts) {
                    throw e;
                }

                System.out.println(
                        "Database write failed. Retrying attempt "
                                + (attempt + 1)
                                + " after "
                                + backoffMillis
                                + "ms"
                );

                try {

                    Thread.sleep(backoffMillis);

                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    throw new RuntimeException(
                            interruptedException
                    );
                }

                backoffMillis *= 2;
            }
        }
    }
}