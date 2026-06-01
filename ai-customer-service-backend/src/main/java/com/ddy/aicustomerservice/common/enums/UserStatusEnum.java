package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
public enum UserStatusEnum {

    ENABLED("ENABLED", "正常"),

    DISABLED("DISABLED", "禁用");

    private final String code;

    private final String name;

    UserStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}