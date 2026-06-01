package com.ddy.aicustomerservice.module.admin.ailog.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:07
 **/
import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * AI知识召回日志分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiRetrievalLogPageQuery extends PageQuery {

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
     * 用户问题关键词
     */
    private String questionKeyword;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 切片ID
     */
    private Long chunkId;

    /**
     * 最低相似度
     */
    private Double minScore;

    /**
     * 最高相似度
     */
    private Double maxScore;

    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}