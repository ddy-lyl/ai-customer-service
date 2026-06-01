package com.ddy.aicustomerservice.module.document.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 9:42
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档切片响应对象
 */
@Data
public class DocumentChunkVO {

    /**
     * 切片ID
     */
    private Long id;

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
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 切片序号
     */
    private Integer chunkIndex;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 切片内容预览
     */
    private String contentPreview;

    /**
     * 内容哈希
     */
    private String contentHash;

    /**
     * token 数量估算
     */
    private Integer tokenCount;

    /**
     * 向量库中的向量ID
     */
    private String vectorId;

    /**
     * 向量化模型名称
     */
    private String embeddingModel;

    /**
     * 是否已向量化：
     * 0 否
     * 1 是
     */
    private Integer vectorized;

    /**
     * 是否已向量化名称
     */
    private String vectorizedName;

    /**
     * 元数据 JSON
     */
    private String metadataJson;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}