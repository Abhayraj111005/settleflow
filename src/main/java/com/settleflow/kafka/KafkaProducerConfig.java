package com.settleflow.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private Map<String, Object> producerProperties() {

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );

        return properties;
    }

    @Bean
    public ProducerFactory<String, SettlementCreatedEvent>
    settlementProducerFactory() {

        return new DefaultKafkaProducerFactory<>(
                producerProperties()
        );
    }

    @Bean
    public KafkaTemplate<String, SettlementCreatedEvent>
    settlementKafkaTemplate(
            ProducerFactory<String, SettlementCreatedEvent> producerFactory) {

        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ProducerFactory<String, TransactionCreatedEvent>
    transactionProducerFactory() {

        return new DefaultKafkaProducerFactory<>(
                producerProperties()
        );
    }

    @Bean
    public KafkaTemplate<String, TransactionCreatedEvent>
    transactionKafkaTemplate(
            ProducerFactory<String, TransactionCreatedEvent> producerFactory) {

        return new KafkaTemplate<>(producerFactory);
    }
}