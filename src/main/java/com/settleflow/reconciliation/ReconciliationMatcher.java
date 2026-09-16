package com.settleflow.reconciliation;

import com.settleflow.entity.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class ReconciliationMatcher {

    public List<ReconciliationResult> reconcile(
            List<Transaction> internalTransactions,
            List<ExternalRecord> externalRecords) {

        /*
         * ---------------------------------------------------------
         * STEP 1: Build HashMap of internal transactions
         * ---------------------------------------------------------
         *
         * Key:
         *     referenceId
         *
         * Value:
         *     Transaction
         */
        Map<String, Transaction> internalByReferenceId =
                new HashMap<>();

        for (Transaction transaction : internalTransactions) {

            internalByReferenceId.put(
                    transaction.getReferenceId(),
                    transaction
            );
        }

        /*
         * ---------------------------------------------------------
         * STEP 2: Group external records by referenceId
         * ---------------------------------------------------------
         *
         * One referenceId can now have multiple external records.
         *
         * Example:
         *
         * REF-1001 -> [₹60, ₹40]
         */
        Map<String, List<ExternalRecord>> externalByReferenceId =
                new HashMap<>();

        for (ExternalRecord externalRecord : externalRecords) {

            externalByReferenceId
                    .computeIfAbsent(
                            externalRecord.getReferenceId(),
                            key -> new ArrayList<>()
                    )
                    .add(externalRecord);
        }

        /*
         * ---------------------------------------------------------
         * STEP 3: Prepare result list
         * ---------------------------------------------------------
         */
        List<ReconciliationResult> results =
                new ArrayList<>();

        /*
         * ---------------------------------------------------------
         * STEP 4: Track references found externally
         * ---------------------------------------------------------
         */
        Set<String> matchedExternalReferences =
                new HashSet<>();

        /*
         * ---------------------------------------------------------
         * STEP 5: Process each external reference group
         * ---------------------------------------------------------
         */
        for (Map.Entry<String, List<ExternalRecord>> entry
                : externalByReferenceId.entrySet()) {

            String referenceId = entry.getKey();

            List<ExternalRecord> externalGroup =
                    entry.getValue();

            Transaction internalTransaction =
                    internalByReferenceId.get(referenceId);

            /*
             * -----------------------------------------------------
             * CASE 1: No internal transaction
             * -----------------------------------------------------
             */
            if (internalTransaction == null) {

                /*
                 * We keep the first external record in the result
                 * because ReconciliationResult currently supports
                 * one ExternalRecord.
                 *
                 * The grouping logic is still applied for matching.
                 */
                results.add(
                        new ReconciliationResult(
                                referenceId,
                                null,
                                externalGroup.get(0),
                                ReconciliationStatus.UNMATCHED
                        )
                );

                continue;
            }

            matchedExternalReferences.add(referenceId);

            /*
             * -----------------------------------------------------
             * STEP 6: Calculate total external amount
             * -----------------------------------------------------
             */
            BigDecimal externalTotal =
                    externalGroup.stream()
                            .map(ExternalRecord::getAmount)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            BigDecimal internalAmount =
                    internalTransaction.getAmount();

            /*
             * -----------------------------------------------------
             * CASE 2: Single external record + exact amount
             * -----------------------------------------------------
             */
            if (externalGroup.size() == 1
                    && internalAmount.compareTo(externalTotal) == 0) {

                results.add(
                        new ReconciliationResult(
                                referenceId,
                                internalTransaction,
                                externalGroup.get(0),
                                ReconciliationStatus.MATCHED
                        )
                );

                continue;
            }

            /*
             * -----------------------------------------------------
             * CASE 3: Multiple external records + exact total
             * -----------------------------------------------------
             */
            if (externalGroup.size() > 1
                    && internalAmount.compareTo(externalTotal) == 0) {

                results.add(
                        new ReconciliationResult(
                                referenceId,
                                internalTransaction,
                                externalGroup.get(0),
                                ReconciliationStatus.PARTIAL_MATCH_RESOLVED
                        )
                );

                continue;
            }

            /*
             * -----------------------------------------------------
             * CASE 4: Reference exists but total amount differs
             * -----------------------------------------------------
             */
            results.add(
                    new ReconciliationResult(
                            referenceId,
                            internalTransaction,
                            externalGroup.get(0),
                            ReconciliationStatus.AMOUNT_MISMATCH
                    )
            );
        }

        /*
         * ---------------------------------------------------------
         * STEP 7: Find internal orphan transactions
         * ---------------------------------------------------------
         */
        for (Transaction transaction : internalTransactions) {

            String referenceId =
                    transaction.getReferenceId();

            if (!matchedExternalReferences.contains(referenceId)) {

                results.add(
                        new ReconciliationResult(
                                referenceId,
                                transaction,
                                null,
                                ReconciliationStatus.UNMATCHED
                        )
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * STEP 8: Return complete reconciliation result
         * ---------------------------------------------------------
         */
        return results;
    }
}