package com.settleflow.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.listener.ContainerProperties;

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
                "settlement-transaction-consumer-group"
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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent>
    transactionKafkaListenerContainerFactory(
            ConsumerFactory<String, TransactionCreatedEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, TransactionCreatedEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL
        );

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

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SettlementCreatedEvent>
    settlementKafkaListenerContainerFactory(
            ConsumerFactory<String, SettlementCreatedEvent> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, SettlementCreatedEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        return factory;
    }
}