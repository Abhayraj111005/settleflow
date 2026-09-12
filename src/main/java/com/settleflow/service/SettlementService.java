package com.settleflow.service;

import com.settleflow.entity.Settlement;
import com.settleflow.entity.SettlementStatus;
import com.settleflow.kafka.SettlementCreatedEvent;
import com.settleflow.kafka.SettlementStatusUpdatedEvent;
import com.settleflow.repository.SettlementRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;

    private final KafkaTemplate<String, SettlementCreatedEvent>
            kafkaTemplate;

    private final KafkaTemplate<String, SettlementStatusUpdatedEvent>
            statusUpdatedKafkaTemplate;

    public SettlementService(
            SettlementRepository settlementRepository,
            KafkaTemplate<String, SettlementCreatedEvent> kafkaTemplate,
            KafkaTemplate<String, SettlementStatusUpdatedEvent>
                    statusUpdatedKafkaTemplate) {

        this.settlementRepository = settlementRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.statusUpdatedKafkaTemplate = statusUpdatedKafkaTemplate;
    }

    // ---------------------------------------------------------
    // Create Settlement
    // ---------------------------------------------------------

    @Transactional
    public Settlement createSettlement(
            String merchantId,
            BigDecimal amount) {

        // New settlement automatically starts as PENDING
        Settlement settlement = new Settlement();

        settlement.setId(UUID.randomUUID());
        settlement.setMerchantId(merchantId);
        settlement.setAmount(amount);
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

        // Publish settlement-created event
        kafkaTemplate.send(
                "settlement-created",
                savedSettlement.getId().toString(),
                event
        );

        return savedSettlement;
    }

    // ---------------------------------------------------------
    // Update Settlement Status
    // ---------------------------------------------------------

    @Transactional
    public Settlement updateStatus(
            UUID settlementId,
            SettlementStatus newStatus) {

        // 1. Load settlement from PostgreSQL
        Settlement settlement =
                settlementRepository.findById(settlementId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Settlement not found: "
                                                + settlementId
                                ));

        // 2. Capture the old state BEFORE transition
        SettlementStatus previousStatus =
                settlement.getStatus();

        // 3. Perform the domain transition.
        //    Invalid transitions are rejected here.
        settlement.transitionTo(newStatus);

        // 4. Save the new state to PostgreSQL
        Settlement savedSettlement =
                settlementRepository.save(settlement);

        // 5. Create status update event
        SettlementStatusUpdatedEvent event =
                new SettlementStatusUpdatedEvent(
                        savedSettlement.getId(),
                        previousStatus,
                        savedSettlement.getStatus()
                );

        // 6. Publish status update event
        statusUpdatedKafkaTemplate.send(
                "settlement.status.updated",
                savedSettlement.getId().toString(),
                event
        );

        return savedSettlement;
    }
}