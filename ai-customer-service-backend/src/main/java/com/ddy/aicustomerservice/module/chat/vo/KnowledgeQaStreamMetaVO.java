package com.ddy.aicustomerservice.module.chat.vo;

import lombok.Data;

import java.util.List;

/**
 * 知识库流式问答 - meta 事件载荷
 *
 * 流的第一帧，告诉前端：
 * 1. 本次会话的 sessionId（用于前端继续聊天）
 * 2. 已落库的 userMessageId
 * 3. RAG 召回了哪些切片（供"知识来源"展示）
 */
@Data
public class KnowledgeQaStreamMetaVO {

    private Long sessionId;

    private Long userMessageId;

    private String questionText;

    private Integer retrievedCount;

    private List<KnowledgeSourceVO> sources;
}
