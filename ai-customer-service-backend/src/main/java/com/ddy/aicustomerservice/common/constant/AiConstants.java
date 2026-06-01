package com.ddy.aicustomerservice.common.constant;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:07
 **/


/**
 * AI 相关常量
 */
public class AiConstants {

    private AiConstants() {
    }

    /**
     * 默认召回知识片段数量
     */
    public static final int DEFAULT_TOP_K = 5;

    /**
     * 默认相似度阈值
     *
     * 低于这个分数的知识片段可以认为相关性较弱。
     */
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.70;

    /**
     * 默认文档切片大小
     */
    public static final int DEFAULT_CHUNK_SIZE = 800;

    /**
     * 默认切片重叠长度
     *
     * 作用：
     * 避免上下文在切片边界处被截断。
     */
    public static final int DEFAULT_CHUNK_OVERLAP = 100;

    /**
     * AI 默认系统提示词
     */
    public static final String DEFAULT_SYSTEM_PROMPT = """
            你是一个专业、严谨的电商售后智能客服助手。
            请优先基于知识库内容回答用户问题。
            如果知识库中没有明确依据，不要编造政策。
            如果问题涉及订单、工单或售后处理，请引导用户提供必要信息。
            """;
}
