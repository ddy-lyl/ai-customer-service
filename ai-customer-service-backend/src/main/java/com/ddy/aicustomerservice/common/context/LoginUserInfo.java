package com.ddy.aicustomerservice.common.context;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:33
 **/

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息
 *
 * 不是数据库实体类。
 * 只是用于保存当前请求中的登录用户身份。
 */
@Data
public class LoginUserInfo {

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
     *
     * 例如：
     * USER、STAFF、ADMIN
     */
    private List<String> roles;

    /**
     * 判断当前用户是否拥有某个角色
     */
    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }
}
