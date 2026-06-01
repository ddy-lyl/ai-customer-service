package com.ddy.aicustomerservice.module.chat.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:55
 **/
import lombok.Data;

import java.util.List;

/**
 * 知识库问答响应对象
 */
@Data
public class KnowledgeQaResponseVO {

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 用户消息ID
     */
    private Long userMessageId;

    /**
     * AI回复消息ID
     */
    private Long assistantMessageId;

    /**
     * 用户问题
     */
    private String questionText;

    /**
     * AI回答
     */
    private String answer;

    /**
     * 召回数量
     */
    private Integer retrievedCount;

    /**
     * 知识来源
     */
    private List<KnowledgeSourceVO> sources;

    private String intent;

    private String modelName;

    private Boolean handoffTriggered;

    private String serviceMode;

    private String handoffNotice;

    private Long handoffTicketId;

    private String handoffTicketNo;
}
