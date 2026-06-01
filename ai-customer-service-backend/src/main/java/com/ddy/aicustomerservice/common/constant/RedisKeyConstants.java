package com.ddy.aicustomerservice.common.constant;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:06
 **/


/**
 * Redis Key 常量
 *
 * Redis 中的 key 必须统一命名。
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 项目统一前缀
     */
    public static final String PROJECT_PREFIX = "ai-customer-service:";

    /**
     * 登录用户缓存
     *
     * 完整 key：
     * ai-customer-service:login:user:{userId}
     */
    public static final String LOGIN_USER_PREFIX = PROJECT_PREFIX + "login:user:";

    /**
     * 登录 Token 黑名单
     *
     * 用户退出登录后，可以把 token 加入黑名单。
     */
    public static final String TOKEN_BLACKLIST_PREFIX = PROJECT_PREFIX + "token:blacklist:";

    /**
     * 验证码缓存
     *
     * 完整 key：
     * ai-customer-service:captcha:{uuid}
     */
    public static final String CAPTCHA_PREFIX = PROJECT_PREFIX + "captcha:";

    /**
     * 热点知识缓存
     *
     * 完整 key：
     * ai-customer-service:knowledge:hot:{knowledgeId}
     */
    public static final String HOT_KNOWLEDGE_PREFIX = PROJECT_PREFIX + "knowledge:hot:";

    /**
     * AI 会话上下文缓存
     *
     * 完整 key：
     * ai-customer-service:chat:context:{sessionId}
     */
    public static final String CHAT_CONTEXT_PREFIX = PROJECT_PREFIX + "chat:context:";

    /**
     * 默认验证码过期时间，单位：分钟
     */
    public static final long CAPTCHA_EXPIRE_MINUTES = 5L;

    /**
     * 登录用户缓存过期时间，单位：分钟
     */
    public static final long LOGIN_USER_EXPIRE_MINUTES = 120L;

    /**
     * 热点知识缓存过期时间，单位：分钟
     */
    public static final long HOT_KNOWLEDGE_EXPIRE_MINUTES = 30L;

    /**
     * AI 会话上下文缓存过期时间，单位：分钟
     */
    public static final long CHAT_CONTEXT_EXPIRE_MINUTES = 60L;
}
