package com.ddy.aicustomerservice.module.ticket.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 16:24
 **/
import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单流转记录分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TicketFlowPageQuery extends PageQuery {

    /**
     * 工单ID
     */
    private Long ticketId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人角色：
     * USER、STAFF、ADMIN、AI
     */
    private String operatorRole;

    /**
     * 操作类型：
     * CREATE、ASSIGN、PROCESS、RESOLVE、CLOSE、CANCEL
     */
    private String action;

    /**
     * 原状态
     */
    private String fromStatus;

    /**
     * 新状态
     */
    private String toStatus;
}