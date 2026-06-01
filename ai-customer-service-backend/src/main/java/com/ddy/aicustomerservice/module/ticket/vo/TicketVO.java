package com.ddy.aicustomerservice.module.ticket.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:27
 **/
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单响应对象
 */
@Data
public class TicketVO {

    private Long id;

    /**
     * 工单编号
     */
    private String ticketNo;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 创建用户昵称
     */
    private String userNickname;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 关联聊天会话ID
     *
     * AI 创建的工单会带该字段，前端可一键跳到对应 AI 对话。
     */
    private Long sessionId;

    /**
     * 工单标题
     */
    private String title;

    /**
     * 工单类型编码
     */
    private String type;

    /**
     * 工单类型名称
     */
    private String typeName;

    /**
     * 问题描述
     */
    private String description;

    /**
     * 工单状态编码
     */
    private String status;

    /**
     * 工单状态名称
     */
    private String statusName;

    /**
     * 优先级编码
     */
    private String priority;

    /**
     * 优先级名称
     */
    private String priorityName;

    /**
     * 当前处理客服ID
     */
    private Long staffId;

    /**
     * 当前处理客服昵称
     */
    private String staffNickname;

    /**
     * 工单来源
     */
    private String source;

    /**
     * 工单来源名称
     */
    private String sourceName;

    /**
     * 处理结果
     */
    private String processResult;

    /**
     * 关闭原因
     */
    private String closeReason;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedTime;

    /**
     * 关闭时间
     */
    private LocalDateTime closedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 工单流转记录
     */
    private List<TicketFlowVO> flows;
}
