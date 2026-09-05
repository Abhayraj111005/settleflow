package com.settleflow.kafka;

import com.settleflow.entity.ProcessedEvent;
import com.settleflow.repository.ProcessedEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionCreatedConsumer {

    private final ProcessedEventRepository processedEventRepository;

    public TransactionCreatedConsumer(
            ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

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

        if (processedEventRepository.existsById(event.eventId())) {

            System.out.println(
                    "Duplicate event detected. Skipping: "
                            + event.eventId()
            );

            acknowledgment.acknowledge();
            return;
        }

        System.out.println(
                "Processing new event: " + event.eventId()
        );

        ProcessedEvent processedEvent = new ProcessedEvent();

        processedEvent.setEventId(event.eventId());
        processedEvent.setProcessedAt(LocalDateTime.now());

        processedEventRepository.saveAndFlush(processedEvent);

        System.out.println(
                "Event persisted successfully: "
                        + event.eventId()
        );

        acknowledgment.acknowledge();
    }
}