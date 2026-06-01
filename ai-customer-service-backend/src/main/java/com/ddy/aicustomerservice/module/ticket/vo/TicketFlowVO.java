package com.ddy.aicustomerservice.module.ticket.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:27
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单流转记录响应对象
 */
@Data
public class TicketFlowVO {

    private Long id;

    /**
     * 工单ID
     */
    private Long ticketId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作人角色
     */
    private String operatorRole;

    /**
     * 操作类型
     */
    private String action;

    /**
     * 操作类型名称
     */
    private String actionName;

    /**
     * 原状态
     */
    private String fromStatus;

    /**
     * 原状态名称
     */
    private String fromStatusName;

    /**
     * 新状态
     */
    private String toStatus;

    /**
     * 新状态名称
     */
    private String toStatusName;

    /**
     * 操作备注
     */
    private String remark;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}
