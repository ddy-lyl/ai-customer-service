package com.ddy.aicustomerservice.module.retrieval.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 14:22
 **/


import lombok.Data;

import java.util.List;

/**
 * 知识库向量检索结果响应对象
 */
@Data
public class KnowledgeRetrievalResultVO {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 用户消息ID
     */
    private Long userMessageId;

    /**
     * 用户原始问题
     */
    private String questionText;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 召回数量参数
     */
    private Integer topK;

    /**
     * 相似度阈值
     */
    private Double similarityThreshold;

    /**
     * 是否经过粗召回 + RRF 重排序
     */
    private Boolean rerankEnabled;

    /**
     * 实际召回数量
     */
    private Integer retrievedCount;

    /**
     * 召回切片列表
     */
    private List<RetrievedChunkVO> chunks;

    /**
     * 组装后的上下文文本
     *
     * 第18章会放进 Prompt。
     */
    private String contextText;
}
