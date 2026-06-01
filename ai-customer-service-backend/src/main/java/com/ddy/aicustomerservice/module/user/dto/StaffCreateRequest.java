package com.ddy.aicustomerservice.module.user.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 12:09
 **/

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员创建客服账号请求对象
 */
@Data
public class StaffCreateRequest {

    /**
     * 客服用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4到20个字符之间")
    private String username;

    /**
     * 初始密码
     */
    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度必须在6到30个字符之间")
    private String password;

    /**
     * 客服昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称不能超过30个字符")
    private String nickname;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
}
