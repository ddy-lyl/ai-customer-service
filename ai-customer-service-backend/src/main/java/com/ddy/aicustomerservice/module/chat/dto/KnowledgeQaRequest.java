package com.ddy.aicustomerservice.module.chat.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:54
 **/
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库问答请求对象
 */
@Data
public class KnowledgeQaRequest {

    /**
     * 会话ID
     *
     * 为空：创建新会话
     * 不为空：继续旧会话
     */
    private Long sessionId;

    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题不能超过1000个字符")
    private String questionText;

    /**
     * 指定知识库ID
     *
     * 为空表示检索所有启用知识库。
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
     */
    @DecimalMin(value = "0.0", message = "similarityThreshold 最小为0.0")
    @DecimalMax(value = "1.0", message = "similarityThreshold 最大为1.0")
    private Double similarityThreshold;
}