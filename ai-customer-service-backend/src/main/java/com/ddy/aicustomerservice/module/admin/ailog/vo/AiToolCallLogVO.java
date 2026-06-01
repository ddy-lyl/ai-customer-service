package com.ddy.aicustomerservice.module.admin.ailog.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:08
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI工具调用日志响应对象
 *
 * 对应 ai_tool_call_log 表。
 */
@Data
public class AiToolCallLogVO {

    private Long id;

    private Long sessionId;

    private Long userId;

    private String username;

    private String nickname;

    private Long userMessageId;

    /**
     * QUERY_ORDER / CREATE_TICKET / QUERY_TICKET_STATUS
     */
    private String toolName;

    private String toolNameText;

    /**
     * 工具入参 JSON
     */
    private String requestJson;

    private String requestPreview;

    /**
     * 工具出参 JSON
     */
    private String responseJson;

    private String responsePreview;

    /**
     * SUCCESS / FAILED
     */
    private String status;

    private String statusName;

    private String errorMessage;

    /**
     * 调用耗时，毫秒
     */
    private Long costMillis;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
