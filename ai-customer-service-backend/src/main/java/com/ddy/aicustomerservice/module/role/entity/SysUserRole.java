package com.ddy.aicustomerservice.module.role.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:36
 **/

import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色关联实体类
 *
 * 对应数据库表：sys_user_role
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRole extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;
}
