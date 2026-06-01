package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 工单类型枚举
 */
@Getter
public enum TicketTypeEnum {

    REFUND("REFUND", "退款问题"),

    RETURN_GOODS("RETURN_GOODS", "退货问题"),

    EXCHANGE("EXCHANGE", "换货问题"),

    LOGISTICS("LOGISTICS", "物流问题"),

    INVOICE("INVOICE", "发票问题"),

    PRODUCT_QUALITY("PRODUCT_QUALITY", "商品质量问题"),

    ACCOUNT("ACCOUNT", "账号问题"),

    CONSULTATION("CONSULTATION", "在线咨询转人工"),

    OTHER("OTHER", "其他问题");

    private final String code;

    private final String name;

    TicketTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
