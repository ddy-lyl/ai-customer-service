package com.ddy.aicustomerservice.module.chat.vo;

import lombok.Data;

/**
 * 知识库流式问答 - done 事件载荷
 *
 * 流的最后一帧，告诉前端：
 * 1. AI 完整回复内容
 * 2. AI 回复消息已经落库的 ID（用于后续追溯）
 * 3. 本轮意图（KNOWLEDGE_QA / GENERAL_CHAT）
 * 4. 使用的对话模型
 */
@Data
public class KnowledgeQaStreamDoneVO {

    private Long sessionId;

    private Long assistantMessageId;

    private String fullAnswer;

    private String intent;

    private String modelName;

    private Boolean handoffTriggered;

    private String serviceMode;

    private String handoffNotice;

    private Long handoffTicketId;

    private String handoffTicketNo;
}
