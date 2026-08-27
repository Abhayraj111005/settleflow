package com.settleflow.settleflow.repository;

import com.settleflow.settleflow.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Optional<Settlement> findByIdempotencyKey(String key);
}