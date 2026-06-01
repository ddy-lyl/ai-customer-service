package com.ddy.aicustomerservice.common.enums;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:08
 **/

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum RoleCodeEnum {

    USER("USER", "普通用户"),

    STAFF("STAFF", "客服人员"),

    ADMIN("ADMIN", "管理员");

    private final String code;

    private final String name;

    RoleCodeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
