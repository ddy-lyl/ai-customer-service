package com.ddy.aicustomerservice.common.utils;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:41
 **/

import com.ddy.aicustomerservice.common.constant.RedisKeyConstants;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * Redis Key 工具类
 *
 * 统一生成 Redis Key。
 */
public class RedisKeyUtils {

    private RedisKeyUtils() {
    }

    /**
     * 登录用户缓存 key
     */
    public static String loginUserKey(Long userId) {
        return RedisKeyConstants.LOGIN_USER_PREFIX + userId;
    }

    /**
     * Token 黑名单 key
     *
     * 不直接把完整 token 当 key，避免 key 过长。
     */
    public static String tokenBlacklistKey(String token) {
        String tokenMd5 = DigestUtils.md5DigestAsHex(
                token.getBytes(StandardCharsets.UTF_8)
        );
        return RedisKeyConstants.TOKEN_BLACKLIST_PREFIX + tokenMd5;
    }

    /**
     * 验证码 key
     */
    public static String captchaKey(String uuid) {
        return RedisKeyConstants.CAPTCHA_PREFIX + uuid;
    }

    /**
     * 热点知识 key
     */
    public static String hotKnowledgeKey(Long knowledgeId) {
        return RedisKeyConstants.HOT_KNOWLEDGE_PREFIX + knowledgeId;
    }

    /**
     * AI 会话上下文 key
     */
    public static String chatContextKey(Long sessionId) {
        return RedisKeyConstants.CHAT_CONTEXT_PREFIX + sessionId;
    }
}