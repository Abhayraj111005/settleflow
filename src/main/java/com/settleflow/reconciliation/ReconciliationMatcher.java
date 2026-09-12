package com.settleflow.reconciliation;

import com.settleflow.entity.Transaction;

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
         *
         * This gives us O(1) average lookup time.
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
         * STEP 2: Prepare result list
         * ---------------------------------------------------------
         */

        List<ReconciliationResult> results =
                new ArrayList<>();


        /*
         * ---------------------------------------------------------
         * STEP 3: Track references found externally
         * ---------------------------------------------------------
         *
         * We need this later to identify internal transactions
         * that have no corresponding external record.
         */

        Set<String> matchedExternalReferences =
                new HashSet<>();


        /*
         * ---------------------------------------------------------
         * STEP 4: Process every external record
         * ---------------------------------------------------------
         */

        for (ExternalRecord externalRecord : externalRecords) {

            String referenceId =
                    externalRecord.getReferenceId();

            Transaction internalTransaction =
                    internalByReferenceId.get(referenceId);


            /*
             * -----------------------------------------------------
             * CASE 1: No internal transaction
             * -----------------------------------------------------
             *
             * External record exists, but SettleFlow has no
             * corresponding transaction.
             */

            if (internalTransaction == null) {

                results.add(
                        new ReconciliationResult(
                                referenceId,
                                null,
                                externalRecord,
                                ReconciliationStatus.UNMATCHED
                        )
                );

                continue;
            }


            /*
             * -----------------------------------------------------
             * Internal transaction exists.
             *
             * Mark this reference as seen externally.
             * -----------------------------------------------------
             */

            matchedExternalReferences.add(referenceId);


            /*
             * -----------------------------------------------------
             * CASE 2: Reference exists but amount differs
             * -----------------------------------------------------
             */

            if (internalTransaction.getAmount()
                    .compareTo(externalRecord.getAmount()) != 0) {

                results.add(
                        new ReconciliationResult(
                                referenceId,
                                internalTransaction,
                                externalRecord,
                                ReconciliationStatus.AMOUNT_MISMATCH
                        )
                );

                continue;
            }


            /*
             * -----------------------------------------------------
             * CASE 3: Reference AND amount match
             * -----------------------------------------------------
             */

            results.add(
                    new ReconciliationResult(
                            referenceId,
                            internalTransaction,
                            externalRecord,
                            ReconciliationStatus.MATCHED
                    )
            );
        }


        /*
         * ---------------------------------------------------------
         * STEP 5: Find internal orphan transactions
         * ---------------------------------------------------------
         *
         * These transactions exist internally but were not found
         * in the external bank statement.
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
         * STEP 6: Return complete reconciliation result
         * ---------------------------------------------------------
         */

        return results;
    }
}
