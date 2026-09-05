package com.settleflow.kafka;

import com.settleflow.entity.Transaction;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionCreatedProducer {

    private static final String TOPIC =
            "settlement.transactions.created";

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    public TransactionCreatedProducer(
            KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Transaction transaction) {

        TransactionCreatedEvent event =
                new TransactionCreatedEvent(
                        transaction.getId(),
                        transaction.getSettlementId(),
                        transaction.getAccountId(),
                        transaction.getMerchantId(),
                        transaction.getAmount(),
                        transaction.getStatus(),
                        transaction.getIdempotencyKey(),
                        transaction.getCreatedAt()
                );

        kafkaTemplate.send(
                TOPIC,
                transaction.getAccountId(),
                event
        );
    }
}