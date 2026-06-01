package com.ddy.aicustomerservice.module.document.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:07
 **/
import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档切片实体类
 *
 * 对应数据库表：document_chunk
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_chunk")
public class DocumentChunk extends BaseEntity {

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 切片序号
     */
    private Integer chunkIndex;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 切片内容哈希
     *
     * 后面可以用于去重。
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
     * 是否已向量化
     *
     * 0：否
     * 1：是
     */
    private Integer vectorized;

    /**
     * 元数据 JSON
     *
     * 例如：页码、章节、段落号
     */
    private String metadataJson;
}
