package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 聊天消息角色枚举
 *
 * 注意：
 * 数据库 chat_message.role 允许 TOOL 角色，对应工具调用消息。
 * 工具消息通常由日志体现，但保留枚举值，便于将来扩展。
 */
@Getter
public enum ChatMessageRoleEnum {

    /**
     * 用户消息
     */
    USER("USER", "用户"),

    /**
     * AI 助手消息
     */
    ASSISTANT("ASSISTANT", "AI助手"),

    /**
     * 系统消息
     */
    SYSTEM("SYSTEM", "系统"),

    /**
     * 人工客服消息
     */
    STAFF("STAFF", "人工客服"),

    /**
     * 工具调用消息
     */
    TOOL("TOOL", "工具");

    private final String code;

    private final String name;

    ChatMessageRoleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
