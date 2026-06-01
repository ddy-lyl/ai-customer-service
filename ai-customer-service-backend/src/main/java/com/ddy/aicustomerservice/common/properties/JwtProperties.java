package com.ddy.aicustomerservice.common.properties;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:38
 **/

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性
 *
 * 对应 application.yml 中的：
 * jwt:
 *   secret: xxx
 *   expire-minutes: 120
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 密钥
     *
     * 注意：
     * 实际上线时不要写死在配置文件中，应该放到环境变量。
     */
    private String secret;

    /**
     * Token 过期时间，单位：分钟
     */
    private Long expireMinutes;
}