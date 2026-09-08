package com.settleflow.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DlqReplayConsumer {

    private final DlqReplayService dlqReplayService;

    public DlqReplayConsumer(DlqReplayService dlqReplayService) {
        this.dlqReplayService = dlqReplayService;
    }

    @KafkaListener(
            topics = "settlement.transactions.created.DLQ",
            groupId = "settlement-dlq-replay-group",
            containerFactory = "dlqTransactionKafkaListenerContainerFactory",
            autoStartup = "false"
    )
    public void consume(
            ConsumerRecord<String, TransactionCreatedEvent> record) {

        dlqReplayService.replay(record);
    }
}