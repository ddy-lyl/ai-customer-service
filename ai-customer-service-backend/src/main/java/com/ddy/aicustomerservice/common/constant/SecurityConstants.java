package com.ddy.aicustomerservice.common.constant;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:05
 **/


/**
 * 安全相关常量
 *
 * 统一管理请求头、Token 前缀、角色前缀等内容。
 */
public class SecurityConstants {

    private SecurityConstants() {
    }

    /**
     * 前端请求头中携带 Token 的 Header 名称
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * JWT Token 前缀
     *
     * 标准写法：
     * Authorization: Bearer xxxxxx
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Spring Security 角色前缀
     *
     * Spring Security 的 hasRole('ADMIN') 底层会匹配 ROLE_ADMIN。
     */
    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * 普通用户角色
     */
    public static final String ROLE_USER = "USER";

    /**
     * 客服角色
     */
    public static final String ROLE_STAFF = "STAFF";

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /**
     * 登录接口地址
     */
    public static final String LOGIN_URL = "/api/auth/login";

    /**
     * 注册接口地址
     */
    public static final String REGISTER_URL = "/api/auth/register";
}
