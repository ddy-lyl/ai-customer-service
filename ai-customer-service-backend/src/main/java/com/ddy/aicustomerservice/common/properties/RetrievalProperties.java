package com.ddy.aicustomerservice.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 检索调优配置
 */
@Data
@ConfigurationProperties(prefix = "ai-customer-service.retrieval")
public class RetrievalProperties {

    /**
     * 是否启用粗召回 + 关键词重排序（RRF 融合）
     */
    private boolean rerankEnabled = true;

    /**
     * 粗召回 topK = min(maxCoarseTopK, finalTopK * coarseTopKMultiplier)
     */
    private int coarseTopKMultiplier = 4;

    private int maxCoarseTopK = 20;

    /**
     * 粗召回阈值 = max(0, 用户阈值 - coarseThresholdDelta)
     */
    private double coarseThresholdDelta = 0.15;

    /**
     * 向量分在 RRF 中的权重（关键词分权重 = 1 - vectorWeight）
     */
    private double vectorRrfWeight = 0.7;
}
