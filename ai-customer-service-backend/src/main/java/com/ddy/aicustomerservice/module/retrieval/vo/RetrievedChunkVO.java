package com.ddy.aicustomerservice.module.retrieval.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 14:21
 **/

import lombok.Data;

/**
 * 召回知识切片响应对象
 */
@Data
public class RetrievedChunkVO {

    /**
     * 召回排序
     */
    private Integer rankNo;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 知识库名称
     */
    private String knowledgeBaseName;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 切片ID
     */
    private Long chunkId;

    /**
     * 切片序号
     */
    private Integer chunkIndex;

    /**
     * 切片内容
     */
    private String chunkContent;

    /**
     * 切片内容预览
     */
    private String chunkContentPreview;

    /**
     * 相似度分数
     */
    private Double score;

    /**
     * 向量ID
     */
    private String vectorId;

    /**
     * 向量模型
     */
    private String embeddingModel;

    /**
     * 内容哈希
     */
    private String contentHash;

    /**
     * token数量估算
     */
    private Integer tokenCount;
}