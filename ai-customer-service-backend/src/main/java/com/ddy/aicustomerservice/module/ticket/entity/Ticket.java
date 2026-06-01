package com.ddy.aicustomerservice.module.ticket.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:22
 **/
import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单实体类
 *
 * 对应数据库表：ticket
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket")
public class Ticket extends BaseEntity {

    /**
     * 工单编号
     *
     * 展示给用户看的编号，例如 TK202605170001
     */
    private String ticketNo;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 关联订单号
     *
     * 有些咨询不一定关联订单，所以允许为空。
     */
    private String orderNo;

    /**
     * 关联聊天会话ID
     *
     * 用于打通"AI 会话 -> 工单"的溯源链路：
     * - AI 工具创建的工单会自动写入 sessionId；
     * - 客服在工单详情页可以一键跳到对应的 AI 对话上下文。
     * 用户在 /api/tickets 直接创建的工单为 null。
     */
    private Long sessionId;

    /**
     * 工单标题
     */
    private String title;

    /**
     * 工单类型
     *
     * REFUND、RETURN_GOODS、EXCHANGE、LOGISTICS、INVOICE、PRODUCT_QUALITY、ACCOUNT、OTHER
     */
    private String type;

    /**
     * 问题描述
     */
    private String description;

    /**
     * 工单状态
     *
     * PENDING、PROCESSING、RESOLVED、CLOSED、CANCELLED
     */
    private String status;

    /**
     * 工单优先级
     *
     * LOW、MEDIUM、HIGH、URGENT
     */
    private String priority;

    /**
     * 当前处理客服ID
     */
    private Long staffId;

    /**
     * 工单来源
     *
     * USER、AI
     */
    private String source;

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
}