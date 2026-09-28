package com.settleflow.repository;

import com.settleflow.entity.WalletOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletOperationRepository
        extends JpaRepository<WalletOperation, UUID> {
}
