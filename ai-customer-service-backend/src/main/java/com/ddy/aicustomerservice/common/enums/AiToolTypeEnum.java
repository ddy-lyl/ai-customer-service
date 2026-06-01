package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * AI 工具类型枚举
 */
@Getter
public enum AiToolTypeEnum {

    QUERY_ORDER("QUERY_ORDER", "订单查询工具"),

    CREATE_TICKET("CREATE_TICKET", "工单创建工具"),

    QUERY_TICKET_STATUS("QUERY_TICKET_STATUS", "工单状态查询工具");

    private final String code;

    private final String name;

    AiToolTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
