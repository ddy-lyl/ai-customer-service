package com.ddy.aicustomerservice.common.result;

/**
 * @author 罗亚兰
 * @date 2026/5/15 16:20
 **/

import lombok.Getter;

/**
 * 统一响应状态码枚举
 */
@Getter
public enum ResultCodeEnum {

    /**
     * 请求成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 通用失败
     */
    FAIL(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 未登录或登录过期
     */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /**
     * 没有权限
     */
    FORBIDDEN(403, "没有权限访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在");

    private final Integer code;

    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
