package com.ddy.aicustomerservice.module.role.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:34
 **/

import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体类
 *
 * 对应数据库表：sys_role
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /**
     * 角色编码：
     * USER、STAFF、ADMIN
     */
    private String roleCode;

    /**
     * 角色名称：
     * 普通用户、客服人员、管理员
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;
}
