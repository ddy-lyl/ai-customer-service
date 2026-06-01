package com.ddy.aicustomerservice.module.chat.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:52
 **/
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI聊天会话实体
 *
 * 对应数据库表：chat_session
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题
     *
     * 默认取用户问题前若干字符。
     */
    private String title;

    /**
     * 会话状态：
     * ACTIVE / CLOSED
     */
    private String status;

    /**
     * 服务模式：AI / WAITING_AGENT / HUMAN
     */
    private String serviceMode;

    /**
     * 当前接待客服 ID
     */
    private Long assignedStaffId;

    /**
     * 转人工关联工单 ID
     */
    private Long activeTicketId;

    /**
     * 转人工原因编码
     */
    private String handoffReason;

    /**
     * 转人工时间
     */
    private LocalDateTime handoffTime;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 乐观锁版本（接入会话等并发更新）
     */
    @Version
    private Integer version;
}