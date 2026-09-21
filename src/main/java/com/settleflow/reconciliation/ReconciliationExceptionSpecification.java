package com.settleflow.reconciliation;

import com.settleflow.entity.ReconciliationException;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ReconciliationExceptionSpecification {

    private ReconciliationExceptionSpecification() {
    }

    public static Specification<ReconciliationException> statusEquals(
            ReconciliationExceptionStatus status) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<ReconciliationException> createdAtGreaterThanOrEqual(
            LocalDateTime from) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        from
                );
    }

    public static Specification<ReconciliationException> createdAtLessThan(
            LocalDateTime toExclusive) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(
                        root.get("createdAt"),
                        toExclusive
                );
    }

    public static Specification<ReconciliationException> amountEquals(
            BigDecimal amount) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.equal(
                                root.get("internalAmount"),
                                amount
                        ),
                        criteriaBuilder.equal(
                                root.get("externalAmount"),
                                amount
                        )
                );
    }
}