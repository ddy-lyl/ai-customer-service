package com.ddy.aicustomerservice.module.ticket.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/17 16:25
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单流转时间线响应对象
 *
 * 主要给前端时间线组件使用。
 */
@Data
public class TicketFlowTimelineVO {

    /**
     * 流转记录ID
     */
    private Long id;

    /**
     * 工单ID
     */
    private Long ticketId;

    /**
     * 时间线标题
     *
     * 例如：创建工单、分配工单、处理工单、关闭工单
     */
    private String title;

    /**
     * 时间线描述
     */
    private String description;

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
     * 操作人角色名称
     */
    private String operatorRoleName;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作动作名称
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
     * 状态变化描述
     *
     * 例如：待分配 → 处理中
     */
    private String statusChangeText;

    /**
     * 操作备注
     */
    private String remark;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}
