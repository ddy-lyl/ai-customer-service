package com.ddy.aicustomerservice.common.result;

/**
 * @author 罗亚兰
 * @date 2026/5/15 16:21
 **/

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口返回结果
 *
 * @param <T> data 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功，无数据
     */
    public static <T> Result<T> success() {
        return new Result<>(
                ResultCodeEnum.SUCCESS.getCode(),
                ResultCodeEnum.SUCCESS.getMessage(),
                null
        );
    }

    /**
     * 成功，有数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResultCodeEnum.SUCCESS.getCode(),
                ResultCodeEnum.SUCCESS.getMessage(),
                data
        );
    }

    /**
     * 成功，自定义消息和数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(
                ResultCodeEnum.SUCCESS.getCode(),
                message,
                data
        );
    }

    /**
     * 失败，使用默认失败状态码
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(
                ResultCodeEnum.FAIL.getCode(),
                message,
                null
        );
    }

    /**
     * 失败，自定义状态码和消息
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(
                code,
                message,
                null
        );
    }

    /**
     * 参数错误
     */
    public static <T> Result<T> paramError(String message) {
        return new Result<>(
                ResultCodeEnum.PARAM_ERROR.getCode(),
                message,
                null
        );
    }

    /**
     * 未登录
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(
                ResultCodeEnum.UNAUTHORIZED.getCode(),
                message,
                null
        );
    }

    /**
     * 无权限
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(
                ResultCodeEnum.FORBIDDEN.getCode(),
                message,
                null
        );
    }
}