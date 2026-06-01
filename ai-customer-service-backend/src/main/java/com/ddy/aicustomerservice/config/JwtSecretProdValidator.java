package com.ddy.aicustomerservice.config;

import com.ddy.aicustomerservice.common.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 生产环境强制配置 JWT_SECRET，禁止使用开发默认密钥。
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class JwtSecretProdValidator {

    private static final String DEFAULT_DEV_SECRET =
            "ai-customer-service-secret-key-ai-customer-service-secret-key";

    private static final int MIN_SECRET_LENGTH = 32;

    private final JwtProperties jwtProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void validateJwtSecret() {
        String secret = jwtProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "生产环境必须通过环境变量 JWT_SECRET 配置 JWT 密钥"
            );
        }
        if (DEFAULT_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "生产环境禁止使用默认 JWT 密钥，请设置环境变量 JWT_SECRET"
            );
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "生产环境 JWT_SECRET 长度至少为 " + MIN_SECRET_LENGTH + " 个字符"
            );
        }
        log.info("生产环境 JWT_SECRET 校验通过");
    }
}
