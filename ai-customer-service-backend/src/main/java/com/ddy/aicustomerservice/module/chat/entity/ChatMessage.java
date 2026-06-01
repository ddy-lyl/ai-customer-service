package com.ddy.aicustomerservice.module.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 聊天消息实体
 *
 * 对应数据库表：chat_message
 *
 * 与 chat_session 是 N:1 关系。
 * 一条 USER 消息对应一条 ASSISTANT 消息，可能伴随若干 TOOL 调用日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 用户ID
     *
     * 即便是 ASSISTANT、SYSTEM、TOOL 消息，
     * 也归属在当前会话所属用户名下，方便后台按用户排查。
     */
    private Long userId;

    /**
     * 消息角色：
     * USER / ASSISTANT / STAFF / SYSTEM / TOOL
     */
    private String role;

    /**
     * 发送者 ID（STAFF 消息为客服用户 ID）
     */
    private Long senderId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 用户意图（仅 USER / ASSISTANT 消息使用）
     *
     * 取值：
     * KNOWLEDGE_QA、ORDER_QUERY、TICKET_CREATE、TICKET_STATUS_QUERY、GENERAL_CHAT、UNKNOWN
     *
     * 当前实现方式：
     * AI 调用结束后，根据本轮 ai_tool_call_log 中触发的工具反推意图。
     */
    private String intent;

    /**
     * 实际调用的 AI 模型名称
     *
     * 例如：qwen3:8b、qwen2.5:14b。
     * 仅 ASSISTANT 消息有值。
     */
    private String modelName;

    /**
     * Prompt token 数
     *
     * 当前大模型 API 未稳定回填，预留字段。
     */
    private Integer promptTokens;

    /**
     * Completion token 数
     */
    private Integer completionTokens;
}
