package com.ddy.aicustomerservice.module.ai.tool.context;

/**
 * @author 罗亚兰
 * @date 2026/5/19 22:46
 **/
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 工具调用上下文信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiToolCallContextInfo {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户消息ID
     */
    private Long userMessageId;
}
