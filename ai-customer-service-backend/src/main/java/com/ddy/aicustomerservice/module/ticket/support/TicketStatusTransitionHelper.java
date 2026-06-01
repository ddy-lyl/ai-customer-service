package com.ddy.aicustomerservice.module.ticket.support;

/**
 * @author 罗亚兰
 * @date 2026/5/17 16:26
 **/
import com.ddy.aicustomerservice.common.enums.TicketFlowActionEnum;
import com.ddy.aicustomerservice.common.enums.TicketStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;

/**
 * 工单状态流转校验工具
 *
 * 作用：
 * 统一判断工单状态是否允许变化。
 */
public class TicketStatusTransitionHelper {

    private TicketStatusTransitionHelper() {
    }

    /**
     * 校验状态流转是否合法。
     *
     * @param action 操作动作
     * @param fromStatus 原状态
     * @param toStatus 新状态
     */
    public static void checkTransition(String action, String fromStatus, String toStatus) {
        if (TicketFlowActionEnum.CREATE.getCode().equals(action)) {
            checkCreate(fromStatus, toStatus);
            return;
        }

        if (TicketFlowActionEnum.ASSIGN.getCode().equals(action)) {
            checkAssign(fromStatus, toStatus);
            return;
        }

        if (TicketFlowActionEnum.CLAIM.getCode().equals(action)) {
            checkClaim(fromStatus, toStatus);
            return;
        }

        if (TicketFlowActionEnum.PROCESS.getCode().equals(action)) {
            checkProcess(fromStatus, toStatus);
            return;
        }

        if (TicketFlowActionEnum.RESOLVE.getCode().equals(action)) {
            checkResolve(fromStatus, toStatus);
            return;
        }

        if (TicketFlowActionEnum.CLOSE.getCode().equals(action)) {
            checkClose(fromStatus, toStatus);
            return;
        }

        if (TicketFlowActionEnum.CANCEL.getCode().equals(action)) {
            checkCancel(fromStatus, toStatus);
            return;
        }

        throw new BusinessException(
                ResultCodeEnum.PARAM_ERROR.getCode(),
                "不支持的工单操作：" + action
        );
    }

    /**
     * 创建工单：null -> PENDING
     */
    private static void checkCreate(String fromStatus, String toStatus) {
        if (fromStatus == null
                && TicketStatusEnum.PENDING.getCode().equals(toStatus)) {
            return;
        }

        throwInvalidTransition("创建工单", fromStatus, toStatus);
    }

    /**
     * 分配工单：PENDING -> PROCESSING
     */
    private static void checkAssign(String fromStatus, String toStatus) {
        if (TicketStatusEnum.PENDING.getCode().equals(fromStatus)
                && TicketStatusEnum.PROCESSING.getCode().equals(toStatus)) {
            return;
        }

        throwInvalidTransition("分配工单", fromStatus, toStatus);
    }

    /**
     * 认领工单：PENDING -> PROCESSING（与分配相同的状态约束）
     */
    private static void checkClaim(String fromStatus, String toStatus) {
        checkAssign(fromStatus, toStatus);
    }

    /**
     * 处理工单：PROCESSING -> PROCESSING
     */
    private static void checkProcess(String fromStatus, String toStatus) {
        if (TicketStatusEnum.PROCESSING.getCode().equals(fromStatus)
                && TicketStatusEnum.PROCESSING.getCode().equals(toStatus)) {
            return;
        }

        throwInvalidTransition("处理工单", fromStatus, toStatus);
    }

    /**
     * 标记解决：PROCESSING -> RESOLVED
     */
    private static void checkResolve(String fromStatus, String toStatus) {
        if (TicketStatusEnum.PROCESSING.getCode().equals(fromStatus)
                && TicketStatusEnum.RESOLVED.getCode().equals(toStatus)) {
            return;
        }

        throwInvalidTransition("标记解决", fromStatus, toStatus);
    }

    /**
     * 关闭工单：
     * PENDING -> CLOSED
     * PROCESSING -> CLOSED
     * RESOLVED -> CLOSED
     */
    private static void checkClose(String fromStatus, String toStatus) {
        if (!TicketStatusEnum.CLOSED.getCode().equals(toStatus)) {
            throwInvalidTransition("关闭工单", fromStatus, toStatus);
        }

        if (TicketStatusEnum.PENDING.getCode().equals(fromStatus)
                || TicketStatusEnum.PROCESSING.getCode().equals(fromStatus)
                || TicketStatusEnum.RESOLVED.getCode().equals(fromStatus)) {
            return;
        }

        throwInvalidTransition("关闭工单", fromStatus, toStatus);
    }

    /**
     * 取消工单：
     * PENDING -> CANCELLED
     *
     * 当前项目暂时没有开放取消接口，但先预留规则。
     */
    private static void checkCancel(String fromStatus, String toStatus) {
        if (TicketStatusEnum.PENDING.getCode().equals(fromStatus)
                && TicketStatusEnum.CANCELLED.getCode().equals(toStatus)) {
            return;
        }

        throwInvalidTransition("取消工单", fromStatus, toStatus);
    }

    /**
     * 抛出状态流转异常
     */
    private static void throwInvalidTransition(String actionName,
                                               String fromStatus,
                                               String toStatus) {
        throw new BusinessException(
                ResultCodeEnum.PARAM_ERROR.getCode(),
                actionName + "状态流转不合法：" + fromStatus + " -> " + toStatus
        );
    }
}