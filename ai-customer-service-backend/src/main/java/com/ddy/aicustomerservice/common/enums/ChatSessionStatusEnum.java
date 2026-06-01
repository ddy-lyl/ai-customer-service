package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 聊天会话状态枚举
 */
@Getter
public enum ChatSessionStatusEnum {

    /**
     * 正常会话
     */
    ACTIVE("ACTIVE", "正常"),

    /**
     * 已关闭
     */
    CLOSED("CLOSED", "已关闭");

    private final String code;

    private final String name;

    ChatSessionStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}