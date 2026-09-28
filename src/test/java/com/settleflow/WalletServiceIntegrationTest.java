package com.settleflow;

import com.settleflow.entity.LedgerEntry;
import com.settleflow.entity.LedgerEntryType;
import com.settleflow.entity.Wallet;
import com.settleflow.entity.WalletOperation;
import com.settleflow.entity.WalletOperationType;
import com.settleflow.repository.LedgerEntryRepository;
import com.settleflow.repository.WalletOperationRepository;
import com.settleflow.repository.WalletRepository;
import com.settleflow.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletOperationRepository walletOperationRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void shouldCompleteWalletHoldReleaseCommitLifecycle() {
        Wallet wallet = walletService.createWallet(
                new BigDecimal("1000.00")
        );

        walletService.hold(
                wallet.getId(),
                new BigDecimal("300.00")
        );

        Wallet afterHold = walletRepository.findById(wallet.getId())
                .orElseThrow();

        assertThat(afterHold.getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(afterHold.getHeldAmount())
                .isEqualByComparingTo("300.00");
        assertThat(afterHold.getAvailableBalance())
                .isEqualByComparingTo("700.00");

        walletService.release(
                wallet.getId(),
                new BigDecimal("100.00")
        );

        Wallet afterRelease = walletRepository.findById(wallet.getId())
                .orElseThrow();

        assertThat(afterRelease.getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(afterRelease.getHeldAmount())
                .isEqualByComparingTo("200.00");
        assertThat(afterRelease.getAvailableBalance())
                .isEqualByComparingTo("800.00");

        walletService.commit(
                wallet.getId(),
                new BigDecimal("200.00")
        );

        Wallet afterCommit = walletRepository.findById(wallet.getId())
                .orElseThrow();

        assertThat(afterCommit.getBalance())
                .isEqualByComparingTo("800.00");
        assertThat(afterCommit.getHeldAmount())
                .isEqualByComparingTo("0.00");
        assertThat(afterCommit.getAvailableBalance())
                .isEqualByComparingTo("800.00");

        List<WalletOperation> operations =
                walletOperationRepository.findAll();

        assertThat(operations)
                .hasSize(3);

        assertOperationLedger(
                operations,
                WalletOperationType.HOLD,
                "300.00"
        );

        assertOperationLedger(
                operations,
                WalletOperationType.RELEASE,
                "100.00"
        );

        assertOperationLedger(
                operations,
                WalletOperationType.COMMIT,
                "200.00"
        );
    }

    private void assertOperationLedger(
            List<WalletOperation> operations,
            WalletOperationType type,
            String expectedAmount
    ) {
        WalletOperation operation = operations.stream()
                .filter(item -> item.getType() == type)
                .findFirst()
                .orElseThrow();

        List<LedgerEntry> entries =
                ledgerEntryRepository.findByWalletOperationId(
                        operation.getId()
                );

        assertThat(entries).hasSize(2);

        assertThat(entries)
                .extracting(LedgerEntry::getEntryType)
                .containsExactlyInAnyOrder(
                        LedgerEntryType.DEBIT,
                        LedgerEntryType.CREDIT
                );

        assertThat(entries)
                .extracting(LedgerEntry::getAmount)
                .allSatisfy(amount ->
                        assertThat(amount)
                                .isEqualByComparingTo(expectedAmount)
                );
    }
}
