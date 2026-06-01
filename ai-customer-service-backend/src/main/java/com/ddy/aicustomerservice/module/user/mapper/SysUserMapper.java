package com.ddy.aicustomerservice.module.user.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:37
 **/

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
