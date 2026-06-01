package com.ddy.aicustomerservice.module.auth.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:46
 **/

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息响应对象
 */
@Data
public class CurrentUserVO {

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
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户状态
     */
    private String status;

    /**
     * 角色编码列表
     */
    private List<String> roles;
}