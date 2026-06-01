package com.ddy.aicustomerservice.module.user.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:32
 **/

import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 对应数据库表：sys_user
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码，数据库中保存 BCrypt 加密后的密文
     */
    private String password;

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
     * 用户状态：
     * ENABLED 正常
     * DISABLED 禁用
     */
    private String status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
}
