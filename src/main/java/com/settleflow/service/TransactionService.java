package com.settleflow.service;

import com.settleflow.entity.LedgerEntry;
import com.settleflow.entity.LedgerEntryType;
import com.settleflow.entity.Transaction;
import com.settleflow.kafka.TransactionCreatedProducer;
import com.settleflow.repository.LedgerEntryRepository;
import com.settleflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionCreatedProducer transactionCreatedProducer;

    public TransactionService(
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            TransactionCreatedProducer transactionCreatedProducer) {

        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionCreatedProducer = transactionCreatedProducer;
    }

    @Transactional
    public Transaction create(Transaction transaction) {

        /*
         * Generate transaction ID if the caller did not provide one.
         */
        if (transaction.getId() == null) {
            transaction.setId(UUID.randomUUID());
        }

        /*
         * Save the transaction.
         */
        Transaction savedTransaction =
                transactionRepository.save(transaction);

        /*
         * Create DEBIT ledger entry.
         */
        LedgerEntry debit = new LedgerEntry();

        debit.setId(UUID.randomUUID());
        debit.setTransactionId(savedTransaction.getId());
        debit.setAmount(savedTransaction.getAmount());
        debit.setEntryType(LedgerEntryType.DEBIT);
        debit.setCreatedAt(LocalDateTime.now());

        /*
         * Create CREDIT ledger entry.
         */
        LedgerEntry credit = new LedgerEntry();

        credit.setId(UUID.randomUUID());
        credit.setTransactionId(savedTransaction.getId());
        credit.setAmount(savedTransaction.getAmount());
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setCreatedAt(LocalDateTime.now());

        /*
         * Persist both ledger entries.
         */
        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        /*
         * Validate the double-entry invariant
         * before allowing the transaction to complete.
         */
        validateLedgerBalance(savedTransaction.getId());

        /*
         * Only publish the Kafka event after the database
         * representation has passed the ledger invariant.
         */
        transactionCreatedProducer.publish(savedTransaction);

        return savedTransaction;
    }

    /**
     * Validates the double-entry accounting invariant.
     *
     * Required invariant:
     *
     * 1. Exactly two ledger entries
     * 2. Exactly one DEBIT
     * 3. Exactly one CREDIT
     * 4. DEBIT amount == CREDIT amount
     */
    private void validateLedgerBalance(UUID transactionId) {

        List<LedgerEntry> entries =
                ledgerEntryRepository.findByTransactionId(transactionId);

        /*
         * A transaction must have exactly two ledger entries.
         */
        if (entries.size() != 2) {
            throw new IllegalStateException(
                    "Transaction must have exactly two ledger entries"
            );
        }

        /*
         * There must be exactly one DEBIT.
         */
        long debitCount = entries.stream()
                .filter(entry ->
                        entry.getEntryType() == LedgerEntryType.DEBIT)
                .count();

        /*
         * There must be exactly one CREDIT.
         */
        long creditCount = entries.stream()
                .filter(entry ->
                        entry.getEntryType() == LedgerEntryType.CREDIT)
                .count();

        if (debitCount != 1 || creditCount != 1) {
            throw new IllegalStateException(
                    "Transaction must have exactly one debit and one credit"
            );
        }

        /*
         * Find the DEBIT entry.
         */
        LedgerEntry debit = entries.stream()
                .filter(entry ->
                        entry.getEntryType() == LedgerEntryType.DEBIT)
                .findFirst()
                .orElseThrow();

        /*
         * Find the CREDIT entry.
         */
        LedgerEntry credit = entries.stream()
                .filter(entry ->
                        entry.getEntryType() == LedgerEntryType.CREDIT)
                .findFirst()
                .orElseThrow();

        /*
         * The two sides of the ledger must balance.
         *
         * BigDecimal must be compared using compareTo(),
         * not equals(), because equals() also considers scale.
         */
        if (debit.getAmount().compareTo(credit.getAmount()) != 0) {
            throw new IllegalStateException(
                    "Ledger entries are not balanced"
            );
        }
    }
}

