package com.ddy.aicustomerservice.common.exception;

/**
 * @author 罗亚兰
 * @date 2026/5/15 16:24
 **/

import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常
 *
 * 用于处理业务规则不满足的情况。
 * 例如：
 * 1. 用户不存在
 * 2. 密码错误
 * 3. 工单状态不允许修改
 * 4. 用户无权查看该订单
 */

@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 使用默认失败状态码
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCodeEnum.FAIL.getCode();
    }

    /**
     * 自定义错误码
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用 ResultCodeEnum
     */
    public BusinessException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }
}
