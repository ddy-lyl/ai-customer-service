package com.ddy.aicustomerservice.module.user.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 12:10
 **/

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户状态修改请求对象
 */
@Data
public class UserStatusUpdateRequest {

    /**
     * 用户状态：
     * ENABLED 正常
     * DISABLED 禁用
     */
    @NotBlank(message = "用户状态不能为空")
    private String status;
}