package com.ddy.aicustomerservice.module.auth.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:55
 **/

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddy.aicustomerservice.common.constant.SecurityConstants;
import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.common.enums.UserStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.common.security.UserTokenVersionService;
import com.ddy.aicustomerservice.common.utils.JwtUtils;
import com.ddy.aicustomerservice.common.utils.RedisKeyUtils;
import com.ddy.aicustomerservice.module.auth.dto.LoginRequest;
import com.ddy.aicustomerservice.module.auth.dto.RegisterRequest;
import com.ddy.aicustomerservice.module.auth.service.AuthService;
import com.ddy.aicustomerservice.module.auth.vo.CurrentUserVO;
import com.ddy.aicustomerservice.module.auth.vo.LoginResponse;
import com.ddy.aicustomerservice.module.role.entity.SysRole;
import com.ddy.aicustomerservice.module.role.entity.SysUserRole;
import com.ddy.aicustomerservice.module.role.mapper.SysRoleMapper;
import com.ddy.aicustomerservice.module.role.mapper.SysUserRoleMapper;
import com.ddy.aicustomerservice.module.user.entity.SysUser;
import com.ddy.aicustomerservice.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 认证业务实现类
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtils jwtUtils;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserTokenVersionService userTokenVersionService;

    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "两次输入的密码不一致"
            );
        }

        SysUser existUser = selectByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "用户名已存在"
            );
        }

        if (StringUtils.hasText(request.getPhone())) {
            Long phoneCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getPhone, request.getPhone())
            );

            if (phoneCount > 0) {
                throw new BusinessException(
                        ResultCodeEnum.PARAM_ERROR.getCode(),
                        "手机号已被使用"
                );
            }
        }

        if (StringUtils.hasText(request.getEmail())) {
            Long emailCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getEmail, request.getEmail())
            );

            if (emailCount > 0) {
                throw new BusinessException(
                        ResultCodeEnum.PARAM_ERROR.getCode(),
                        "邮箱已被使用"
                );
            }
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(UserStatusEnum.ENABLED.getCode());

        sysUserMapper.insert(user);

        SysRole userRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, RoleCodeEnum.USER.getCode())
                        .last("LIMIT 1")
        );

        if (userRole == null) {
            throw new BusinessException("系统角色 USER 不存在，请先初始化 sys_role 表");
        }

        SysUserRole userRoleRelation = new SysUserRole();
        userRoleRelation.setUserId(user.getId());
        userRoleRelation.setRoleId(userRole.getId());

        sysUserRoleMapper.insert(userRoleRelation);
    }

    /**
     * 用户登录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        SysUser user = selectByUsername(request.getUsername());

        if (user == null) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "用户名或密码错误"
            );
        }

        if (UserStatusEnum.DISABLED.getCode().equals(user.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN.getCode(),
                    "账号已被禁用，请联系管理员"
            );
        }

        boolean passwordMatch = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatch) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "用户名或密码错误"
            );
        }

        List<String> roles = queryRoleCodesByUserId(user.getId());

        if (roles == null || roles.isEmpty()) {
            throw new BusinessException("当前用户没有分配角色，无法登录");
        }

        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setUserId(user.getId());
        loginUserInfo.setUsername(user.getUsername());
        loginUserInfo.setNickname(user.getNickname());
        loginUserInfo.setRoles(roles);

        String tokenVersion = userTokenVersionService.getOrCreateTokenVersion(user.getId());
        String token = jwtUtils.generateToken(loginUserInfo, tokenVersion);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRoles(roles);

        return response;
    }

    /**
     * 获取当前用户信息
     *
     * 现在不再手动解析请求头。
     * JwtAuthenticationFilter 已经把用户信息写入 SecurityContext 的 Principal。
     */
    @Override
    public CurrentUserVO getCurrentUser() {
        Long userId = resolveCurrentUserId();

        SysUser user = sysUserMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "用户不存在"
            );
        }

        CurrentUserVO vo = new CurrentUserVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setRoles(queryRoleCodesByUserId(user.getId()));

        return vo;
    }

    /**
     * 退出登录
     *
     * JWT 本身是无状态的。
     * 如果想让退出登录立即生效，需要把当前 token 加入 Redis 黑名单。
     *
     * 未携带 Token 时直接返回，避免未登录用户调用接口报 500。
     */
    @Override
    public void logout(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return;
        }

        String token = extractToken(authorizationHeader);

        if (!jwtUtils.isTokenValid(token)) {
            return;
        }

        Date expiration = jwtUtils.getExpiration(token);
        long remainMillis = expiration.getTime() - System.currentTimeMillis();

        if (remainMillis <= 0) {
            return;
        }

        String blacklistKey = RedisKeyUtils.tokenBlacklistKey(token);

        stringRedisTemplate.opsForValue().set(
                blacklistKey,
                "logout",
                Duration.ofMillis(remainMillis)
        );
    }

    /**
     * 根据用户名查询用户
     */
    /**
     * 从 Spring Security 上下文解析当前用户 ID（与 JwtAuthenticationFilter 写入的 Principal 一致）。
     */
    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof LoginUserInfo loginUserInfo)) {
            throw new BusinessException(
                    ResultCodeEnum.UNAUTHORIZED.getCode(),
                    "请先登录"
            );
        }
        return loginUserInfo.getUserId();
    }

    private SysUser selectByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .last("LIMIT 1")
        );
    }

    /**
     * 查询用户角色编码
     */
    private List<String> queryRoleCodesByUserId(Long userId) {
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

        List<SysRole> roles = sysRoleMapper.selectByIds(roleIds);

        return roles.stream()
                .map(SysRole::getRoleCode)
                .toList();
    }

    /**
     * 提取真正的 Token
     *
     * 调用前必须保证 authorizationHeader 非空。
     */
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return authorizationHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
        }

        return authorizationHeader;
    }
}