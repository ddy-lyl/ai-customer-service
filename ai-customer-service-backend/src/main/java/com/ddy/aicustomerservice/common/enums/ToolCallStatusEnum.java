package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * AI 工具调用状态枚举
 */
@Getter
public enum ToolCallStatusEnum {

    SUCCESS("SUCCESS", "调用成功"),

    FAILED("FAILED", "调用失败");

    private final String code;

    private final String name;

    ToolCallStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
