package com.ddy.aicustomerservice.module.chat.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:54
 **/
import lombok.Data;

/**
 * 知识来源响应对象
 */
@Data
public class KnowledgeSourceVO {

    /**
     * 排名
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
     * 切片内容预览
     */
    private String chunkContentPreview;

    /**
     * 相似度分数
     */
    private Double score;
}
