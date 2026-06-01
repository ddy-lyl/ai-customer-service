package com.ddy.aicustomerservice.module.chat.support;

import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.config.AiCustomerServiceProperties;
import org.springframework.util.StringUtils;

/**
 * 知识库问答 Prompt 构建器
 *
 * 之前 system-prompt 是硬编码在这里的，现在统一从
 * application.yml 的 ai-customer-service.chat.system-prompt
 * 注入。这里只保留拼接用户 prompt 的逻辑和回退兜底。
 */
public class KnowledgePromptBuilder {

    /**
     * 拼接到各角色 system prompt 前的语言约束。
     */
    public static final String LANGUAGE_RULE = """
            【语言要求】必须使用简体中文回复，禁止出现繁体字（例如：請、選、協、處、後、訂單、發貨）。
            工具返回的订单状态名称请原样使用，其余说明文字一律用简体。
            """;

    /**
     * 当配置文件没有提供 system-prompt 时的兜底文案。
     */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一名专业、礼貌、有耐心的售后智能客服助手。
            - 优先依据【知识库片段】回答；
            - 涉及订单/工单数据时必须调用工具，不要编造；
            - 知识库无法回答时，主动建议用户创建工单；
            - 用简体中文，简洁分点回答，不输出推理过程。
            """;

    /**
     * 为 system prompt 加上统一的语言约束。
     */
    public static String withLanguageRule(String systemPrompt) {
        if (!StringUtils.hasText(systemPrompt)) {
            return LANGUAGE_RULE + DEFAULT_SYSTEM_PROMPT;
        }
        return LANGUAGE_RULE + systemPrompt;
    }

    private KnowledgePromptBuilder() {
    }

    /**
     * 选择 system prompt：优先配置，缺省使用兜底。
     */
    public static String resolveSystemPrompt(String configuredPrompt) {
        String base = StringUtils.hasText(configuredPrompt)
                ? configuredPrompt
                : DEFAULT_SYSTEM_PROMPT;
        return withLanguageRule(base);
    }

    /**
     * 按登录主角色选择 system prompt（USER / STAFF / ADMIN）。
     */
    public static String resolveSystemPromptForRole(RoleCodeEnum role,
                                                    AiCustomerServiceProperties.Chat chatConfig) {
        if (chatConfig == null) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        String prompt = switch (role) {
            case ADMIN -> chatConfig.getAdminSystemPrompt();
            case STAFF -> chatConfig.getStaffSystemPrompt();
            case USER -> chatConfig.getUserSystemPrompt();
        };
        if (StringUtils.hasText(prompt)) {
            return withLanguageRule(prompt);
        }
        return resolveSystemPrompt(chatConfig.getSystemPrompt());
    }

    /**
     * 用户提示词（拼接知识库召回片段 + 用户原始问题）
     */
    public static String buildUserPrompt(String questionText, String contextText) {
        return buildUserPrompt(RoleCodeEnum.USER, questionText, contextText);
    }

    public static String buildUserPrompt(RoleCodeEnum role, String questionText, String contextText) {
        String safeContext = StringUtils.hasText(contextText)
                ? contextText
                : "当前没有检索到相关知识库内容。";

        String roleHint = switch (role) {
            case ADMIN -> """
                    你是管理员的运营分析助手。请结合工具返回的平台数据与知识库片段回答。
                    涉及统计、热点、工单分类时请调用平台概览/热点问题工具，不要编造数字。
                    """;
            case STAFF -> """
                    你是客服工作台助手。请结合知识库与工具，帮助客服处理已分配或待认领工单。
                    查订单、查工单、列工单时请调用工具；可给出推荐回复与处理方案，但不要代替客服正式承诺退款金额。
                    """;
            case USER -> """
                    你是面向消费者的售后助手。涉及订单/工单必须调用工具，禁止编造。
                    用户申请售后/建工单时，CREATE_TICKET 必须带有效 orderNo；未说明订单号时先 LIST_MY_ORDERS 让用户选单。
                    知识库无法解决且工具无效时，引导用户创建工单。
                    """;
        };

        return """
                %s

                【知识库片段】
                %s

                【用户问题】
                %s

                请严格按 system 中的规则回答。
                """.formatted(roleHint, safeContext, questionText);
    }
}
