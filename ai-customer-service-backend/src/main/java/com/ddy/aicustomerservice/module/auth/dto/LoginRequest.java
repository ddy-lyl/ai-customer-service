package com.ddy.aicustomerservice.module.auth.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:48
 **/

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求对象
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
