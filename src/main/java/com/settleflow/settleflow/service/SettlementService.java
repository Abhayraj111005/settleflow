package com.settleflow.settleflow.service;

import com.settleflow.settleflow.dto.SettlementRequestDTO;
import com.settleflow.settleflow.dto.SettlementResponseDTO;
import com.settleflow.settleflow.entity.Settlement;
import com.settleflow.settleflow.entity.SettlementStatus;
import com.settleflow.settleflow.repository.SettlementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;

    public SettlementService(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    public SettlementResponseDTO createSettlement(SettlementRequestDTO dto) {

        // 1. Validate merchantId
        if (dto.getMerchantId() == null || dto.getMerchantId().isBlank()) {
            throw new IllegalArgumentException("merchantId is required");
        }

        // 2. Validate amount
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        // 3. Check for duplicate idempotency key
        if (settlementRepository.findByIdempotencyKey(dto.getIdempotencyKey()).isPresent()) {
            throw new IllegalStateException("Duplicate idempotency key");
        }

        // 4. Convert Request DTO → Entity
        Settlement settlement = new Settlement();

        settlement.setMerchantId(dto.getMerchantId());
        settlement.setAmount(dto.getAmount());
        settlement.setOrderRef(dto.getOrderRef());
        settlement.setIdempotencyKey(dto.getIdempotencyKey());

        // 5. Set server-controlled fields
        settlement.setStatus(SettlementStatus.PENDING);
        settlement.setCreatedAt(LocalDateTime.now());

        // 6. Save entity
        Settlement savedSettlement = settlementRepository.save(settlement);

        // 7. Convert Entity → Response DTO
        return new SettlementResponseDTO(
                savedSettlement.getSettlementId(),
                savedSettlement.getStatus().name(),
                savedSettlement.getCreatedAt()
        );
    }
}