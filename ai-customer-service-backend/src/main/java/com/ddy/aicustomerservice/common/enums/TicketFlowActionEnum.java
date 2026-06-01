package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 工单流转动作枚举
 */
@Getter
public enum TicketFlowActionEnum {

    /**
     * 创建工单
     */
    CREATE("CREATE", "创建工单"),

    /**
     * 分配工单
     */
    ASSIGN("ASSIGN", "分配工单"),

    /**
     * 客服主动认领待处理工单
     */
    CLAIM("CLAIM", "认领工单"),

    /**
     * 处理工单
     */
    PROCESS("PROCESS", "处理工单"),

    /**
     * 标记已解决
     */
    RESOLVE("RESOLVE", "标记已解决"),

    /**
     * 关闭工单
     */
    CLOSE("CLOSE", "关闭工单"),

    /**
     * 取消工单
     */
    CANCEL("CANCEL", "取消工单");

    private final String code;

    private final String name;

    TicketFlowActionEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}