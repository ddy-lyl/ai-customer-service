package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 工单优先级枚举
 */
@Getter
public enum TicketPriorityEnum {

    LOW("LOW", "低"),

    MEDIUM("MEDIUM", "中"),

    HIGH("HIGH", "高"),

    URGENT("URGENT", "紧急");

    private final String code;

    private final String name;

    TicketPriorityEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
