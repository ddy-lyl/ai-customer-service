package com.ddy.aicustomerservice.module.retrieval.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/19 14:22
 **/

import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI知识召回日志实体
 *
 * 对应数据库表：ai_retrieval_log
 *
 * 一条召回切片 = 一条日志记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_retrieval_log")
public class AiRetrievalLog extends BaseEntity {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 用户ID
     */
    private Long userId;

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
     * 召回切片内容快照
     */
    private String chunkContent;

    /**
     * 相似度分数
     */
    private Double score;

    /**
     * 召回排序
     */
    private Integer rankNo;
}
