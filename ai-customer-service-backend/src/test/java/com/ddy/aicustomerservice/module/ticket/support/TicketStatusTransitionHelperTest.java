package com.ddy.aicustomerservice.module.ticket.support;

import com.ddy.aicustomerservice.common.enums.TicketFlowActionEnum;
import com.ddy.aicustomerservice.common.enums.TicketStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketStatusTransitionHelperTest {

    @Test
    void create_pending_isValid() {
        assertDoesNotThrow(() -> TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.CREATE.getCode(),
                null,
                TicketStatusEnum.PENDING.getCode()
        ));
    }

    @Test
    void claim_pendingToProcessing_isValid() {
        assertDoesNotThrow(() -> TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.CLAIM.getCode(),
                TicketStatusEnum.PENDING.getCode(),
                TicketStatusEnum.PROCESSING.getCode()
        ));
    }

    @Test
    void resolve_fromPending_isInvalid() {
        assertThrows(BusinessException.class, () -> TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.RESOLVE.getCode(),
                TicketStatusEnum.PENDING.getCode(),
                TicketStatusEnum.RESOLVED.getCode()
        ));
    }

    @Test
    void close_fromResolved_isValid() {
        assertDoesNotThrow(() -> TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.CLOSE.getCode(),
                TicketStatusEnum.RESOLVED.getCode(),
                TicketStatusEnum.CLOSED.getCode()
        ));
    }

    @Test
    void cancel_pendingToCancelled_isValid() {
        assertDoesNotThrow(() -> TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.CANCEL.getCode(),
                TicketStatusEnum.PENDING.getCode(),
                TicketStatusEnum.CANCELLED.getCode()
        ));
    }

    @Test
    void unknownAction_throws() {
        assertThrows(BusinessException.class, () -> TicketStatusTransitionHelper.checkTransition(
                "UNKNOWN",
                TicketStatusEnum.PENDING.getCode(),
                TicketStatusEnum.PROCESSING.getCode()
        ));
    }
}
