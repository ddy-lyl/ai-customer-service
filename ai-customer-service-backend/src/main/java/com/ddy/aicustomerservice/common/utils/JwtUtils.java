package com.ddy.aicustomerservice.common.utils;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:39
 **/

import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类
 *
 * 作用：
 * 1. 生成 Token
 * 2. 解析 Token
 * 3. 判断 Token 是否过期
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    private static final String CLAIM_USER_ID = "userId";

    private static final String CLAIM_USERNAME = "username";

    private static final String CLAIM_NICKNAME = "nickname";

    private static final String CLAIM_ROLES = "roles";

    private static final String CLAIM_TOKEN_VERSION = "tv";

    /**
     * 生成 JWT Token
     */
    public String generateToken(LoginUserInfo loginUserInfo, String tokenVersion) {
        Date now = new Date();
        Date expireTime = new Date(
                now.getTime() + jwtProperties.getExpireMinutes() * 60 * 1000
        );

        return Jwts.builder()
                .subject(loginUserInfo.getUsername())
                .claim(CLAIM_USER_ID, loginUserInfo.getUserId())
                .claim(CLAIM_USERNAME, loginUserInfo.getUsername())
                .claim(CLAIM_NICKNAME, loginUserInfo.getNickname())
                .claim(CLAIM_ROLES, loginUserInfo.getRoles())
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(now)
                .expiration(expireTime)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析 Token，获取 Claims
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中解析登录用户信息
     */
    public LoginUserInfo parseLoginUser(String token) {
        Claims claims = parseClaims(token);

        LoginUserInfo loginUserInfo = new LoginUserInfo();
        loginUserInfo.setUserId(getLongClaim(claims, CLAIM_USER_ID));
        loginUserInfo.setUsername(claims.get(CLAIM_USERNAME, String.class));
        loginUserInfo.setNickname(claims.get(CLAIM_NICKNAME, String.class));
        loginUserInfo.setRoles(getRoles(claims));

        return loginUserInfo;
    }

    /**
     * 从 Token 中读取版本号（用于与 Redis 比对）
     */
    public String getTokenVersion(String token) {
        try {
            Claims claims = parseClaims(token);
            Object value = claims.get(CLAIM_TOKEN_VERSION);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断 Token 是否有效
     */
    public boolean isTokenValid(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Token 过期时间
     */
    public Date getExpiration(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration();
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从 Claims 中读取 Long 类型字段
     */
    private Long getLongClaim(Claims claims, String key) {
        Object value = claims.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        return Long.valueOf(value.toString());
    }

    /**
     * 从 Claims 中读取角色列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);

        if (roles instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .toList();
        }

        return List.of();
    }
}