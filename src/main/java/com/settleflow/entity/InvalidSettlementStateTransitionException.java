package com.settleflow.entity;

public class InvalidSettlementStateTransitionException
        extends RuntimeException {

    public InvalidSettlementStateTransitionException(
            SettlementStatus current,
            SettlementStatus requested) {

        super(
                "Invalid settlement state transition: "
                        + current
                        + " -> "
                        + requested
        );
    }
}