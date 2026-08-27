package com.settleflow.settleflow.service;

import com.settleflow.settleflow.entity.Settlement;
import com.settleflow.settleflow.entity.SettlementStatus;
import com.settleflow.settleflow.repository.SettlementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;

    public SettlementService(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    public Settlement createSettlement(
            String merchantId,
            BigDecimal amount,
            String orderRef,
            String idempotencyKey) {

        return settlementRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {

                    Settlement settlement = new Settlement();

                    settlement.setMerchantId(merchantId);
                    settlement.setAmount(amount);
                    settlement.setOrderRef(orderRef);
                    settlement.setStatus(SettlementStatus.PENDING);
                    settlement.setIdempotencyKey(idempotencyKey);
                    settlement.setCreatedAt(LocalDateTime.now());

                    return settlementRepository.save(settlement);
                });
    }
}