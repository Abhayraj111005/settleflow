package com.settleflow.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DlqReplayService {

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    public DlqReplayService(
            @Qualifier("transactionKafkaTemplate")
            KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void replay(
            ConsumerRecord<String, TransactionCreatedEvent> record) {

        String originalTopic = getOriginalTopic(record);

        kafkaTemplate.send(
                originalTopic,
                record.key(),
                record.value()
        );
    }

    private String getOriginalTopic(
            ConsumerRecord<String, TransactionCreatedEvent> record) {

        String dlqTopic = record.topic();

        if (!dlqTopic.endsWith(".DLQ")) {
            throw new IllegalArgumentException(
                    "Record is not from a DLQ topic: " + dlqTopic
            );
        }

        return dlqTopic.substring(
                0,
                dlqTopic.length() - ".DLQ".length()
        );
    }
}