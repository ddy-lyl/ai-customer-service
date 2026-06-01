package com.ddy.aicustomerservice.common.security;

import com.ddy.aicustomerservice.common.constant.SecurityConstants;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.utils.JwtUtils;
import com.ddy.aicustomerservice.common.utils.RedisKeyUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * 处理顺序：
 * 1. 没有或非 Bearer Token：直接放行，由后续 Security 规则决定是否拦截。
 * 2. Token 在 Redis 黑名单：放行（视同未登录），后续受保护接口会被 EntryPoint 拒绝。
 * 3. Token 无效（过期/签名错误）：放行，理由同上。
 * 4. Token 有效：解析用户信息，写入 SecurityContextHolder + LoginUserContext。
 *
 * 注意：
 * STATELESS 模式下 SecurityContext 由 Spring Security 在请求结束时自动清理，
 * 这里不需要手动 clearContext()。LoginUserContext 走 ThreadLocal 必须显式清理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserTokenVersionService userTokenVersionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authorizationHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
            if (!StringUtils.hasText(authorizationHeader)) {
                filterChain.doFilter(request, response);
                return;
            }

            String header = authorizationHeader.trim();
            if (!header.regionMatches(true, 0, SecurityConstants.TOKEN_PREFIX, 0,
                    SecurityConstants.TOKEN_PREFIX.length())) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
            if (!StringUtils.hasText(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (isTokenBlacklisted(token)) {
                log.debug("Token 已加入黑名单，视为未登录");
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtUtils.isTokenValid(token)) {
                log.debug("Token 无效或已过期, uri={}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String tokenVersion = jwtUtils.getTokenVersion(token);
                if (!StringUtils.hasText(tokenVersion)) {
                    log.debug("Token 缺少版本号，视为未登录");
                    filterChain.doFilter(request, response);
                    return;
                }

                LoginUserInfo loginUserInfo = jwtUtils.parseLoginUser(token);

                if (!userTokenVersionService.isTokenVersionValid(
                        loginUserInfo.getUserId(), tokenVersion)) {
                    log.debug("Token 版本已失效, userId={}", loginUserInfo.getUserId());
                    filterChain.doFilter(request, response);
                    return;
                }

                List<SimpleGrantedAuthority> authorities = loginUserInfo.getRoles()
                        .stream()
                        .map(role -> new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role))
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                loginUserInfo,
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                LoginUserContext.set(loginUserInfo);
            }

            filterChain.doFilter(request, response);

        } finally {
            // SecurityContextHolder 在 STATELESS 模式下由 Spring Security 自动清理。
            // LoginUserContext 基于 ThreadLocal，必须手动清理避免线程复用串号。
            LoginUserContext.remove();
        }
    }

    /**
     * 判断 Token 是否在黑名单。
     *
     * Redis 不可用时仅记录警告并放行 JWT 校验，避免因 Redis 宕机导致全员无法登录。
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String blacklistKey = RedisKeyUtils.tokenBlacklistKey(token);
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey));
        } catch (Exception ex) {
            log.warn("Redis 黑名单查询失败，跳过黑名单校验: {}", ex.getMessage());
            return false;
        }
    }
}
