package com.ddy.aicustomerservice.module.user.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 12:07
 **/

import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageQuery {

    /**
     * 用户名，支持模糊查询
     */
    private String username;

    /**
     * 昵称，支持模糊查询
     */
    private String nickname;

    /**
     * 手机号，支持模糊查询
     */
    private String phone;

    /**
     * 用户状态：
     * ENABLED / DISABLED
     */
    private String status;

    /**
     * 角色编码：
     * USER / STAFF / ADMIN
     */
    private String roleCode;
}