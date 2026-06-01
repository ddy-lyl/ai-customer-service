package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatusEnum {

    WAIT_PAY("WAIT_PAY", "待支付"),

    WAIT_SHIPPING("WAIT_SHIPPING", "待发货"),

    SHIPPED("SHIPPED", "已发货"),

    RECEIVED("RECEIVED", "已签收"),

    REFUNDING("REFUNDING", "退款中"),

    REFUNDED("REFUNDED", "已退款"),

    CLOSED("CLOSED", "已关闭");

    private final String code;

    private final String name;

    OrderStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
