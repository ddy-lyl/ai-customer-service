package com.ddy.aicustomerservice.module.ticket.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:23
 **/
import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单流转记录实体类
 *
 * 对应数据库表：ticket_flow
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_flow")
public class TicketFlow extends BaseEntity {

    /**
     * 工单ID
     */
    private Long ticketId;

    /**
     * 操作人ID
     *
     * AI 创建工单时可以为空。
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作人角色
     *
     * USER、STAFF、ADMIN、AI
     */
    private String operatorRole;

    /**
     * 操作类型
     *
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

    /**
     * 操作备注
     */
    private String remark;
}
