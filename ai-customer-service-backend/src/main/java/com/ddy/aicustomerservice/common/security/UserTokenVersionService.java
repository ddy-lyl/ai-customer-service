package com.ddy.aicustomerservice.common.security;

import com.ddy.aicustomerservice.common.constant.RedisKeyConstants;
import com.ddy.aicustomerservice.common.properties.JwtProperties;
import com.ddy.aicustomerservice.common.utils.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 用户 Token 版本号：禁用账号或重置密码后递增，使旧 JWT 立即失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTokenVersionService {

    private final StringRedisTemplate stringRedisTemplate;

    private final JwtProperties jwtProperties;

    /**
     * 登录时获取或初始化版本号（不踢掉其他已登录设备）。
     */
    public String getOrCreateTokenVersion(Long userId) {
        String key = RedisKeyUtils.loginUserKey(userId);
        try {
            String current = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(current)) {
                return current;
            }
            return refreshTokenVersion(userId);
        } catch (Exception ex) {
            log.warn("Redis Token 版本读取失败，使用临时版本: {}", ex.getMessage());
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * 强制失效该用户所有已签发 Token（禁用、重置密码等）。
     */
    public void invalidateUserTokens(Long userId) {
        refreshTokenVersion(userId);
    }

    /**
     * 校验 JWT 中的版本号是否与 Redis 一致。
     */
    public boolean isTokenVersionValid(Long userId, String tokenVersion) {
        if (userId == null || !StringUtils.hasText(tokenVersion)) {
            return false;
        }
        try {
            String current = stringRedisTemplate.opsForValue()
                    .get(RedisKeyUtils.loginUserKey(userId));
            return StringUtils.hasText(current) && current.equals(tokenVersion);
        } catch (Exception ex) {
            log.warn("Redis Token 版本校验失败，跳过版本校验: {}", ex.getMessage());
            return true;
        }
    }

    private String refreshTokenVersion(Long userId) {
        String version = String.valueOf(System.currentTimeMillis());
        long expireMinutes = jwtProperties.getExpireMinutes() != null
                ? jwtProperties.getExpireMinutes()
                : RedisKeyConstants.LOGIN_USER_EXPIRE_MINUTES;
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyUtils.loginUserKey(userId),
                    version,
                    Duration.ofMinutes(expireMinutes)
            );
        } catch (Exception ex) {
            log.warn("Redis Token 版本写入失败: {}", ex.getMessage());
        }
        return version;
    }
}
