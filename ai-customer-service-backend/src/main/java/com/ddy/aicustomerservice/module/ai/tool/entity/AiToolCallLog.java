package com.ddy.aicustomerservice.module.ai.tool.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/19 22:48
 **/
import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 工具调用日志实体
 *
 * 对应数据库表：ai_tool_call_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_tool_call_log")
public class AiToolCallLog extends BaseEntity {

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
     * 工具名称：
     * QUERY_ORDER、CREATE_TICKET、QUERY_TICKET_STATUS
     */
    private String toolName;

    /**
     * 工具入参 JSON
     */
    private String requestJson;

    /**
     * 工具出参 JSON
     */
    private String responseJson;

    /**
     * 调用状态：
     * SUCCESS、FAILED
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 调用耗时，单位毫秒
     */
    private Long costMillis;
}
