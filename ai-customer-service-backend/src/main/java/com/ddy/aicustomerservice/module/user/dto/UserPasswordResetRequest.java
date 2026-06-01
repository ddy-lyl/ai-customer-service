package com.ddy.aicustomerservice.module.user.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 12:11
 **/

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码请求对象
 */
@Data
public class UserPasswordResetRequest {

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度必须在6到30个字符之间")
    @JsonAlias("password")
    private String newPassword;
}
