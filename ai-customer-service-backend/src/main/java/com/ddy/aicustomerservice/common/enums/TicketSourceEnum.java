package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 工单来源枚举
 */
@Getter
public enum TicketSourceEnum {

    /**
     * 用户手动创建
     */
    USER("USER", "用户创建"),

    /**
     * AI 自动创建
     */
    AI("AI", "AI创建"),

    /**
     * 客服代用户建单
     */
    STAFF("STAFF", "客服代建");

    private final String code;

    private final String name;

    TicketSourceEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}