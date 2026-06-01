package com.ddy.aicustomerservice.module.retrieval.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/19 14:11
 **/
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库向量检索请求对象
 *
 * 注意：
 * sessionId 和 userMessageId 现在可以传 0。
 * 后面接正式聊天模块时，再传真实 chat_session.id 和 chat_message.id。
 */
@Data
public class KnowledgeRetrievalRequest {

    /**
     * 会话ID
     *
     * 第17章管理员测试时传 0。
     * 第18章正式AI聊天时传真实 sessionId。
     */
    private Long sessionId;

    /**
     * 用户消息ID
     *
     * 第17章管理员测试时传 0。
     * 第18章正式AI聊天时传真实 userMessageId。
     */
    private Long userMessageId;

    /**
     * 用户原始问题
     */
    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题不能超过1000个字符")
    private String questionText;

    /**
     * 指定知识库ID
     *
     * 不传表示检索所有启用知识库。
     */
    private Long knowledgeBaseId;

    /**
     * 召回数量
     */
    @Min(value = 1, message = "topK 最小为1")
    @Max(value = 20, message = "topK 最大为20")
    private Integer topK;

    /**
     * 相似度阈值
     *
     * 0.0 ~ 1.0。
     * 值越高，要求越相似。
     */
    @DecimalMin(value = "0.0", message = "similarityThreshold 最小为0.0")
    @DecimalMax(value = "1.0", message = "similarityThreshold 最大为1.0")
    private Double similarityThreshold;
}
