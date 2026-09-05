package com.settleflow.kafka;

import com.settleflow.entity.ProcessedEvent;
import com.settleflow.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
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

        System.out.println(
                "Processing event: " + event.eventId()
        );

        ProcessedEvent processedEvent = new ProcessedEvent();

        processedEvent.setEventId(event.eventId());
        processedEvent.setProcessedAt(LocalDateTime.now());

        try {

            saveWithRetry(processedEvent);

            System.out.println(
                    "Event persisted successfully: "
                            + event.eventId()
            );

        } catch (DataIntegrityViolationException e) {

            System.out.println(
                    "Duplicate event detected. Skipping: "
                            + event.eventId()
            );
        }

        acknowledgment.acknowledge();
    }

    private void saveWithRetry(ProcessedEvent processedEvent) {

        int maxAttempts = 3;
        long backoffMillis = 100;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                processedEventRepository.saveAndFlush(processedEvent);

                return;

            } catch (DataIntegrityViolationException e) {

                // Duplicate event.
                // Do not retry because the database
                // has already told us this event exists.
                throw e;

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

    throw new RuntimeException(interruptedException);
}

backoffMillis *= 2;

            }
        }
    }
}