package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 会话服务模式：AI 自动回复 / 等待人工 / 人工接待中
 */
@Getter
public enum ChatServiceModeEnum {

    AI("AI", "AI 接待"),
    WAITING_AGENT("WAITING_AGENT", "等待人工"),
    HUMAN("HUMAN", "人工接待中");

    private final String code;

    private final String name;

    ChatServiceModeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
