package com.settleflow.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Properties;

@RestController
@RequestMapping("/admin/dlq")
public class DlqReplayController {

    private final DlqReplayService dlqReplayService;

    public DlqReplayController(DlqReplayService dlqReplayService) {
        this.dlqReplayService = dlqReplayService;
    }

    @PostMapping("/replay")
    public String replay(
            @RequestParam int partition,
            @RequestParam long offset) {

        String dlqTopic = "settlement.transactions.created.DLQ";

        Properties properties = new Properties();

        properties.put(
                "bootstrap.servers",
                "localhost:9092"
        );

        properties.put(
                "key.deserializer",
                "org.apache.kafka.common.serialization.StringDeserializer"
        );

        properties.put(
                "value.deserializer",
                "org.springframework.kafka.support.serializer.JsonDeserializer"
        );

        properties.put(
                "spring.json.value.default.type",
                TransactionCreatedEvent.class.getName()
        );

        properties.put(
                "spring.json.trusted.packages",
                "com.settleflow.kafka"
        );

        try (KafkaConsumer<String, TransactionCreatedEvent> consumer =
                     new KafkaConsumer<>(properties)) {

            TopicPartition topicPartition =
                    new TopicPartition(dlqTopic, partition);

            consumer.assign(
                    Collections.singletonList(topicPartition)
            );

            consumer.seek(topicPartition, offset);

            var records = consumer.poll(
                    java.time.Duration.ofSeconds(2)
            );

            if (records.isEmpty()) {
                throw new IllegalArgumentException(
                        "No DLQ record found at partition "
                                + partition
                                + ", offset "
                                + offset
                );
            }

            ConsumerRecord<String, TransactionCreatedEvent> record =
                    records.iterator().next();

            dlqReplayService.replay(record);

            return "DLQ record replayed successfully: partition="
                    + partition
                    + ", offset="
                    + offset;
        }
    }
}