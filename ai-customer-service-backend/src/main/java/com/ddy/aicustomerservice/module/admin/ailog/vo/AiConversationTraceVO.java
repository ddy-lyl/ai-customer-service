package com.ddy.aicustomerservice.module.admin.ailog.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:09
 **/
import lombok.Data;

import java.util.List;

/**
 * AI会话完整排查链路响应对象
 */
@Data
public class AiConversationTraceVO {

    /**
     * 会话信息
     */
    private AiChatSessionAdminVO session;

    /**
     * 会话消息
     */
    private List<AiChatMessageAdminVO> messages;

    /**
     * 知识召回日志
     */
    private List<AiRetrievalLogVO> retrievalLogs;

    /**
     * 工具调用日志
     */
    private List<AiToolCallLogVO> toolCallLogs;
}
