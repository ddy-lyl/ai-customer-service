package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 用户意图枚举
 *
 * 在一次 AI 对话结束后回填到 chat_message.intent，
 * 主要根据本轮工具调用情况推断：
 * - 调用了 QUERY_ORDER          -> ORDER_QUERY
 * - 调用了 CREATE_TICKET        -> TICKET_CREATE
 * - 调用了 QUERY_TICKET_STATUS  -> TICKET_STATUS_QUERY
 * - 未调用工具但有知识召回      -> KNOWLEDGE_QA
 * - 完全没有命中               -> GENERAL_CHAT
 */
@Getter
public enum ChatIntentEnum {

    KNOWLEDGE_QA("KNOWLEDGE_QA", "知识库问答"),

    ORDER_QUERY("ORDER_QUERY", "订单查询"),

    TICKET_CREATE("TICKET_CREATE", "创建工单"),

    TICKET_STATUS_QUERY("TICKET_STATUS_QUERY", "工单状态查询"),

    GENERAL_CHAT("GENERAL_CHAT", "通用聊天"),

    UNKNOWN("UNKNOWN", "未知");

    private final String code;

    private final String name;

    ChatIntentEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
