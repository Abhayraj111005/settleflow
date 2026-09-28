package com.settleflow.service;

import com.settleflow.entity.LedgerEntry;
import com.settleflow.entity.LedgerEntryType;
import com.settleflow.entity.Wallet;
import com.settleflow.entity.WalletOperation;
import com.settleflow.entity.WalletOperationType;
import com.settleflow.repository.LedgerEntryRepository;
import com.settleflow.repository.WalletOperationRepository;
import com.settleflow.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletOperationRepository walletOperationRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletOperationRepository walletOperationRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.walletRepository = walletRepository;
        this.walletOperationRepository = walletOperationRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public Wallet createWallet(BigDecimal initialBalance) {
        Wallet wallet = new Wallet(UUID.randomUUID(), initialBalance);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet hold(UUID walletId, BigDecimal amount) {
        Wallet wallet = getWalletForUpdate(walletId);

        wallet.hold(amount);

        WalletOperation operation = createOperation(
                walletId,
                WalletOperationType.HOLD,
                amount
        );

        createBalancedLedgerEntries(operation);

        return wallet;
    }

    @Transactional
    public Wallet release(UUID walletId, BigDecimal amount) {
        Wallet wallet = getWalletForUpdate(walletId);

        wallet.release(amount);

        WalletOperation operation = createOperation(
                walletId,
                WalletOperationType.RELEASE,
                amount
        );

        createBalancedLedgerEntries(operation);

        return wallet;
    }

    @Transactional
    public Wallet commit(UUID walletId, BigDecimal amount) {
        Wallet wallet = getWalletForUpdate(walletId);

        wallet.commit(amount);

        WalletOperation operation = createOperation(
                walletId,
                WalletOperationType.COMMIT,
                amount
        );

        createBalancedLedgerEntries(operation);

        return wallet;
    }

    private Wallet getWalletForUpdate(UUID walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Wallet not found: " + walletId
                        )
                );
    }

    private WalletOperation createOperation(
            UUID walletId,
            WalletOperationType type,
            BigDecimal amount
    ) {
        WalletOperation operation = new WalletOperation(
                UUID.randomUUID(),
                walletId,
                type,
                amount
        );

        return walletOperationRepository.save(operation);
    }

    private void createBalancedLedgerEntries(WalletOperation operation) {
       LedgerEntry debit = LedgerEntry.forWalletOperation(
        UUID.randomUUID(),
        operation.getId(),
        operation.getAmount(),
        LedgerEntryType.DEBIT
);

LedgerEntry credit = LedgerEntry.forWalletOperation(
        UUID.randomUUID(),
        operation.getId(),
        operation.getAmount(),
        LedgerEntryType.CREDIT
);

        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);
    }
}
