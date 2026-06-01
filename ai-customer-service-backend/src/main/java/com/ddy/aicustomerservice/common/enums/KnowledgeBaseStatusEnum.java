package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 知识库状态枚举
 */
@Getter
public enum KnowledgeBaseStatusEnum {

    /**
     * 启用
     */
    ENABLED("ENABLED", "启用"),


    /**
     * 禁用
     */
    DISABLED("DISABLED", "禁用");

    private final String code;

    private final String name;

    KnowledgeBaseStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}