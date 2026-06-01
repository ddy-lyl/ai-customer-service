package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 工单状态枚举
 */
@Getter
public enum TicketStatusEnum {

    PENDING("PENDING", "待分配"),

    PROCESSING("PROCESSING", "处理中"),

    RESOLVED("RESOLVED", "已解决"),

    CLOSED("CLOSED", "已关闭"),

    CANCELLED("CANCELLED", "已取消");

    private final String code;

    private final String name;

    TicketStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
