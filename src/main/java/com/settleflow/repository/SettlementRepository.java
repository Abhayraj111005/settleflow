package com.settleflow.repository;

import com.settleflow.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettlementRepository
        extends JpaRepository<Settlement, UUID> {
}