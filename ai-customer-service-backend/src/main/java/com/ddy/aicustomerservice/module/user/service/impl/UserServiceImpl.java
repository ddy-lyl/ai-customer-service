package com.ddy.aicustomerservice.module.user.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/17 12:13
 **/
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.common.enums.UserStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.role.entity.SysRole;
import com.ddy.aicustomerservice.module.role.entity.SysUserRole;
import com.ddy.aicustomerservice.module.role.mapper.SysRoleMapper;
import com.ddy.aicustomerservice.module.role.mapper.SysUserRoleMapper;
import com.ddy.aicustomerservice.module.user.dto.StaffCreateRequest;
import com.ddy.aicustomerservice.module.user.dto.UserPageQuery;
import com.ddy.aicustomerservice.module.user.dto.UserPasswordResetRequest;
import com.ddy.aicustomerservice.module.user.dto.UserStatusUpdateRequest;
import com.ddy.aicustomerservice.module.user.entity.SysUser;
import com.ddy.aicustomerservice.module.user.mapper.SysUserMapper;
import com.ddy.aicustomerservice.module.user.service.UserService;
import com.ddy.aicustomerservice.common.security.UserTokenVersionService;
import com.ddy.aicustomerservice.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.util.List;
/**
 * 用户业务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final UserTokenVersionService userTokenVersionService;

    /**
     * 管理员分页查询用户
     *
     * 之前的实现是 MP 分页后再用 Java 流过滤 roleCode，
     * 会导致分页 total 失真：返回的 total 是过滤前的，
     * records 却是过滤后的。这里改为在分页前用 roleCode
     * 把目标 userId 先取出来，再用 IN 条件下推到 SQL，
     * 这样 MP 自带的 count 就是过滤后的正确值。
     */
    @Override
    public PageResult<UserVO> pageUsers(UserPageQuery query) {
        Page<SysUser> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(query.getUsername()),
                SysUser::getUsername, query.getUsername());
        wrapper.like(StringUtils.hasText(query.getNickname()),
                SysUser::getNickname, query.getNickname());
        wrapper.like(StringUtils.hasText(query.getPhone()),
                SysUser::getPhone, query.getPhone());
        wrapper.eq(StringUtils.hasText(query.getStatus()),
                SysUser::getStatus, query.getStatus());

        if (StringUtils.hasText(query.getRoleCode())) {
            List<Long> userIdsOfRole = resolveUserIdsByRoleCode(query.getRoleCode());
            if (userIdsOfRole.isEmpty()) {
                return PageResult.of(
                        page.getCurrent(),
                        page.getSize(),
                        0L,
                        0L,
                        List.of()
                );
            }
            wrapper.in(SysUser::getId, userIdsOfRole);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> userPage = sysUserMapper.selectPage(page, wrapper);

        List<UserVO> userVOList = userPage.getRecords()
                .stream()
                .map(this::convertToUserVO)
                .toList();

        return PageResult.of(
                userPage.getCurrent(),
                userPage.getSize(),
                userPage.getTotal(),
                userPage.getPages(),
                userVOList
        );
    }

    /**
     * 根据角色编码反查关联的 userId 列表
     *
     * 当角色不存在或没有关联用户时返回空集合。
     */
    private List<Long> resolveUserIdsByRoleCode(String roleCode) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, roleCode)
                        .last("LIMIT 1")
        );
        if (role == null) {
            return List.of();
        }
        List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, role.getId())
        );
        if (userRoleList == null || userRoleList.isEmpty()) {
            return List.of();
        }
        return userRoleList.stream()
                .map(SysUserRole::getUserId)
                .toList();
    }

    /**
     * 根据用户ID查询用户详情
     */
    @Override
    public UserVO getUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);

        if (user == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "用户不存在"
            );
        }

        return convertToUserVO(user);
    }

    /**
     * 管理员创建客服账号
     *
     * 创建客服账号需要做两件事：
     * 1. 插入 sys_user
     * 2. 绑定 STAFF 角色
     *
     * 所以必须加事务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createStaff(StaffCreateRequest request) {
        checkUsernameUnique(request.getUsername());
        checkPhoneUnique(request.getPhone());
        checkEmailUnique(request.getEmail());

        SysUser staff = new SysUser();
        staff.setUsername(request.getUsername());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setNickname(request.getNickname());
        staff.setPhone(request.getPhone());
        staff.setEmail(request.getEmail());
        staff.setStatus(UserStatusEnum.ENABLED.getCode());

        sysUserMapper.insert(staff);

        bindRole(staff.getId(), RoleCodeEnum.STAFF.getCode());

        return convertToUserVO(staff);
    }

    /**
     * 管理员修改用户状态
     */
    @Override
    public void updateUserStatus(Long id, UserStatusUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(id);

        if (user == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "用户不存在"
            );
        }

        if (!UserStatusEnum.ENABLED.getCode().equals(request.getStatus())
                && !UserStatusEnum.DISABLED.getCode().equals(request.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "用户状态只能是 ENABLED 或 DISABLED"
            );
        }

        /*
         * 防止管理员把自己禁用。
         * 否则当前管理员可能无法继续操作后台。
         */
        Long currentUserId = LoginUserContext.getUserId();

        if (id.equals(currentUserId)
                && UserStatusEnum.DISABLED.getCode().equals(request.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "不能禁用当前登录账号"
            );
        }

        user.setStatus(request.getStatus());
        sysUserMapper.updateById(user);

        if (UserStatusEnum.DISABLED.getCode().equals(request.getStatus())) {
            userTokenVersionService.invalidateUserTokens(id);
        }
    }

    /**
     * 管理员重置用户密码
     */
    @Override
    public void resetPassword(Long id, UserPasswordResetRequest request) {
        SysUser user = sysUserMapper.selectById(id);

        if (user == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "用户不存在"
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        sysUserMapper.updateById(user);
        userTokenVersionService.invalidateUserTokens(id);
    }

    /**
     * 查询客服列表
     */
    @Override
    public List<UserVO> listStaff() {
        SysRole staffRole = selectRoleByCode(RoleCodeEnum.STAFF.getCode());

        List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, staffRole.getId())
        );

        if (userRoleList == null || userRoleList.isEmpty()) {
            return List.of();
        }

        List<Long> staffIds = userRoleList.stream()
                .map(SysUserRole::getUserId)
                .toList();

        List<SysUser> staffList = sysUserMapper.selectByIds(staffIds);

        return staffList.stream()
                .filter(user -> UserStatusEnum.ENABLED.getCode().equals(user.getStatus()))
                .map(this::convertToUserVO)
                .toList();
    }

    /**
     * 当前用户查看自己的信息
     */
    @Override
    public UserVO getCurrentUserProfile() {
        Long currentUserId = LoginUserContext.getUserId();
        return getUserById(currentUserId);
    }

    /**
     * 用户实体转 VO
     */
    private UserVO convertToUserVO(SysUser user) {
        UserVO vo = new UserVO();

        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setStatus(user.getStatus());
        vo.setStatusName(convertUserStatusName(user.getStatus()));
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());

        List<SysRole> roles = queryRolesByUserId(user.getId());

        vo.setRoles(
                roles.stream()
                        .map(SysRole::getRoleCode)
                        .toList()
        );

        vo.setRoleNames(
                roles.stream()
                        .map(SysRole::getRoleName)
                        .toList()
        );

        return vo;
    }

    /**
     * 查询用户角色
     */
    private List<SysRole> queryRolesByUserId(Long userId) {
        List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );

        if (userRoleList == null || userRoleList.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoleList.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        return sysRoleMapper.selectByIds(roleIds);
    }

    /**
     * 绑定角色
     */
    private void bindRole(Long userId, String roleCode) {
        SysRole role = selectRoleByCode(roleCode);

        Long count = sysUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getRoleId, role.getId())
        );

        if (count > 0) {
            return;
        }

        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(role.getId());

        sysUserRoleMapper.insert(relation);
    }

    /**
     * 根据角色编码查询角色
     */
    private SysRole selectRoleByCode(String roleCode) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, roleCode)
                        .last("LIMIT 1")
        );

        if (role == null) {
            throw new BusinessException("系统角色不存在：" + roleCode);
        }

        return role;
    }

    /**
     * 检查用户名唯一
     */
    private void checkUsernameUnique(String username) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (count > 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "用户名已存在"
            );
        }
    }

    /**
     * 检查手机号唯一
     */
    private void checkPhoneUnique(String phone) {
        if (!StringUtils.hasText(phone)) {
            return;
        }

        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phone)
        );

        if (count > 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "手机号已被使用"
            );
        }
    }

    /**
     * 检查邮箱唯一
     */
    private void checkEmailUnique(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, email)
        );

        if (count > 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "邮箱已被使用"
            );
        }
    }

    /**
     * 用户状态名称转换
     */
    private String convertUserStatusName(String status) {
        if (UserStatusEnum.ENABLED.getCode().equals(status)) {
            return UserStatusEnum.ENABLED.getName();
        }

        if (UserStatusEnum.DISABLED.getCode().equals(status)) {
            return UserStatusEnum.DISABLED.getName();
        }

        return "未知状态";
    }
}