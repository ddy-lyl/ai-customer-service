package com.ddy.aicustomerservice.module.role.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:40
 **/

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.role.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}