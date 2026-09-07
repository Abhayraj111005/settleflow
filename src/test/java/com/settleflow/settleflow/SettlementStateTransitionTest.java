package com.settleflow.settleflow;

import com.settleflow.entity.InvalidSettlementStateTransitionException;
import com.settleflow.entity.Settlement;
import com.settleflow.entity.SettlementStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettlementStateTransitionTest {

    @Test
    void shouldAllowPendingToProcessing() {

        Settlement settlement = new Settlement();

        settlement.transitionTo(SettlementStatus.PROCESSING);

        assertEquals(
                SettlementStatus.PROCESSING,
                settlement.getStatus()
        );
    }

    @Test
    void shouldAllowProcessingToCompleted() {

        Settlement settlement = new Settlement();

        settlement.transitionTo(SettlementStatus.PROCESSING);
        settlement.transitionTo(SettlementStatus.COMPLETED);

        assertEquals(
                SettlementStatus.COMPLETED,
                settlement.getStatus()
        );
    }

    @Test
    void shouldAllowProcessingToFailed() {

        Settlement settlement = new Settlement();

        settlement.transitionTo(SettlementStatus.PROCESSING);
        settlement.transitionTo(SettlementStatus.FAILED);

        assertEquals(
                SettlementStatus.FAILED,
                settlement.getStatus()
        );
    }

    @Test
    void shouldRejectCompletedToPending() {

        Settlement settlement = new Settlement();

        settlement.transitionTo(SettlementStatus.PROCESSING);
        settlement.transitionTo(SettlementStatus.COMPLETED);

        InvalidSettlementStateTransitionException exception =
                assertThrows(
                        InvalidSettlementStateTransitionException.class,
                        () -> settlement.transitionTo(
                                SettlementStatus.PENDING
                        )
                );

        assertEquals(
                "Invalid settlement state transition: COMPLETED -> PENDING",
                exception.getMessage()
        );
    }
}