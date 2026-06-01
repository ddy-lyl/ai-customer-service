package com.ddy.aicustomerservice.module.auth.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:45
 **/

import lombok.Data;

import java.util.List;

/**
 * 登录响应对象
 */
@Data
public class LoginResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * Token 类型
     *
     * 前端请求时使用：
     * Authorization: Bearer xxxxxx
     */
    private String tokenType = "Bearer";

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 角色编码列表
     */
    private List<String> roles;
}
