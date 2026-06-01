package com.ddy.aicustomerservice.module.admin.ailog.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:08
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI知识召回日志响应对象
 *
 * 对应 ai_retrieval_log 表。
 */
@Data
public class AiRetrievalLogVO {

    private Long id;

    private Long sessionId;

    private Long userId;

    private String username;

    private String nickname;

    private Long userMessageId;

    private String questionText;

    private String questionPreview;

    private Long knowledgeBaseId;

    private Long documentId;

    private String documentName;

    private Long chunkId;

    private String chunkContent;

    private String chunkContentPreview;

    private Double score;

    private Integer rankNo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}