package com.ddy.aicustomerservice.module.user.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/17 10:37
 **/

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应对象
 *
 * 注意：
 * 不能返回 password。
 */
@Data
public class UserVO {

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
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 用户状态编码：
     * ENABLED / DISABLED
     */
    private String status;

    /**
     * 用户状态名称：
     * 正常 / 禁用
     */
    private String statusName;

    /**
     * 角色编码列表：
     * USER / STAFF / ADMIN
     */
    private List<String> roles;

    /**
     * 角色名称列表：
     * 普通用户 / 客服人员 / 管理员
     */
    private List<String> roleNames;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
