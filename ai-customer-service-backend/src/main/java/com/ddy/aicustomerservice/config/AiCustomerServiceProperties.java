package com.ddy.aicustomerservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 业务侧自定义配置
 *
 * 对应 application.yml:
 * ai-customer-service:
 *   chat:
 *     system-prompt: ...
 *   knowledge:
 *     top-k: 4
 *     similarity-threshold: 0.5
 *
 * 把模型提示词、检索默认参数等业务参数从代码中剥离出来，
 * 调整时不需要改源码。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai-customer-service")
public class AiCustomerServiceProperties {

    private final Chat chat = new Chat();

    private final Knowledge knowledge = new Knowledge();

    private final Handoff handoff = new Handoff();

    @Data
    public static class Chat {

        /**
         * 兼容旧配置：未配置分角色 prompt 时作为默认 system prompt。
         */
        private String systemPrompt;

        /**
         * 普通用户（C 端售后咨询）system prompt。
         */
        private String userSystemPrompt;

        /**
         * 客服人员（工单处理辅助）system prompt。
         */
        private String staffSystemPrompt;

        /**
         * 管理员（运营与知识库治理）system prompt。
         */
        private String adminSystemPrompt;

        /**
         * 是否将本会话历史 USER/ASSISTANT 消息注入大模型（多轮上下文）。
         */
        private Boolean historyEnabled = true;

        /**
         * 最多加载多少条历史消息（仅 USER、ASSISTANT）。
         */
        private Integer maxHistoryMessages = 20;

        /**
         * 历史消息总字符上限（粗略控制 token，超出则从最早的消息丢弃）。
         */
        private Integer maxHistoryChars = 6000;
    }

    @Data
    public static class Knowledge {

        /**
         * 默认召回 top-k
         */
        private Integer topK = 4;

        /**
         * 默认相似度阈值
         */
        private Double similarityThreshold = 0.5;
    }

    @Data
    public static class Handoff {

        private Boolean enabled = true;

        private Boolean ragMissAuto = true;

        private Double lowScoreThreshold = 0.55;

        private Integer repeatMissCount = 2;

        private String userKeywords = "转人工,人工客服,找客服,真人,人工服务";

        private String refusalPhrases = "无法回答,没有相关,建议联系人工,不清楚,无法确定";
    }
}
