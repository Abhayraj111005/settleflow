package com.settleflow.service;

import com.settleflow.entity.Settlement;
import com.settleflow.kafka.SettlementCreatedEvent;
import com.settleflow.repository.SettlementRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final KafkaTemplate<String, SettlementCreatedEvent> kafkaTemplate;

    public SettlementService(
            SettlementRepository settlementRepository,
            KafkaTemplate<String, SettlementCreatedEvent> kafkaTemplate) {

        this.settlementRepository = settlementRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public Settlement createSettlement(
            String merchantId,
            java.math.BigDecimal amount) {

        // Create settlement
        Settlement settlement = new Settlement();

        settlement.setMerchantId(merchantId);
        settlement.setAmount(amount);
        settlement.setStatus("PENDING");
        settlement.setCreatedAt(LocalDateTime.now());

        // Save to PostgreSQL
        Settlement savedSettlement =
                settlementRepository.save(settlement);

        // Create Kafka event
        SettlementCreatedEvent event =
                new SettlementCreatedEvent(
                        savedSettlement.getId(),
                        savedSettlement.getMerchantId(),
                        savedSettlement.getAmount(),
                        null
                );

        // Publish event
        kafkaTemplate.send(
                "settlement-created",
                savedSettlement.getId().toString(),
                event
        );

        return savedSettlement;
    }
}