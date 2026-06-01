package com.ddy.aicustomerservice.module.ai.tool.context;

/**
 * @author 罗亚兰
 * @date 2026/5/19 22:47
 **/
/**
 * AI 工具调用上下文
 *
 * 使用 ThreadLocal 保存本次 AI 对话的上下文。
 */
public class AiToolCallContext {

    private static final ThreadLocal<AiToolCallContextInfo> CONTEXT = new ThreadLocal<>();

    private AiToolCallContext() {
    }

    public static void set(AiToolCallContextInfo contextInfo) {
        CONTEXT.set(contextInfo);
    }

    public static AiToolCallContextInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
