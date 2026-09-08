package com.settleflow.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    // ============================================================
    // Transaction Created Consumer
    // ============================================================

    @Bean
    public ConsumerFactory<String, TransactionCreatedEvent>
    transactionConsumerFactory() {

        JsonDeserializer<TransactionCreatedEvent> jsonDeserializer =
                new JsonDeserializer<>(TransactionCreatedEvent.class);

        jsonDeserializer.addTrustedPackages("com.settleflow.kafka");

        ErrorHandlingDeserializer<TransactionCreatedEvent> deserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "settlement-transaction-consumer-group"
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class
        );

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                deserializer
        );
    }


    // ============================================================
    // Transaction Created Listener
    // ============================================================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent>
    transactionKafkaListenerContainerFactory(
            @Qualifier("transactionConsumerFactory")
            ConsumerFactory<String, TransactionCreatedEvent> consumerFactory,

            @Qualifier("transactionKafkaTemplate")
            KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // Manual acknowledgment
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL
        );

        // ========================================================
        // Dead Letter Publishing Recoverer
        // ========================================================

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        record.topic() + ".DLQ",
                                        record.partition()
                                )
                );

        // ========================================================
        // Retry Configuration
        //
        // Original attempt
        //       ↓
        // Retry 1
        //       ↓ 1 second
        // Retry 2
        //       ↓ 1 second
        // Retry 3
        //       ↓
        // DLQ
        // ========================================================

        FixedBackOff backOff =
                new FixedBackOff(
                        1000L,
                        3L
                );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        backOff
                );

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }


    // ============================================================
    // Settlement Created Consumer
    // ============================================================

    @Bean
    public ConsumerFactory<String, SettlementCreatedEvent>
    settlementConsumerFactory() {

        JsonDeserializer<SettlementCreatedEvent> deserializer =
                new JsonDeserializer<>(SettlementCreatedEvent.class);

        deserializer.addTrustedPackages("com.settleflow.kafka");

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "settlement-service-group"
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                deserializer
        );
    }


    // ============================================================
    // Settlement Created Listener
    // ============================================================

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SettlementCreatedEvent>
    settlementKafkaListenerContainerFactory(
            @Qualifier("settlementConsumerFactory")
            ConsumerFactory<String, SettlementCreatedEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, SettlementCreatedEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        return factory;
    }


    // ============================================================
    // DLQ Transaction Consumer
    // ============================================================

    @Bean
    public ConsumerFactory<String, TransactionCreatedEvent>
    dlqTransactionConsumerFactory() {

        JsonDeserializer<TransactionCreatedEvent> deserializer =
                new JsonDeserializer<>(TransactionCreatedEvent.class);

        deserializer.addTrustedPackages("com.settleflow.kafka");

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "settlement-dlq-replay-group"
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                deserializer
        );
    }


    // ============================================================
    // DLQ Replay Listener
    // ============================================================

    @Bean(name = "dlqTransactionKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent>
    dlqTransactionKafkaListenerContainerFactory(
            @Qualifier("dlqTransactionConsumerFactory")
            ConsumerFactory<String, TransactionCreatedEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        return factory;
    }
}