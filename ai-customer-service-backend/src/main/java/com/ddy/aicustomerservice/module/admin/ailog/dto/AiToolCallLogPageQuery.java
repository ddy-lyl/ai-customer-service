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
 * AI工具调用日志分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiToolCallLogPageQuery extends PageQuery {

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
     * QUERY_ORDER / CREATE_TICKET / QUERY_TICKET_STATUS
     */
    private String toolName;

    /**
     * 调用状态：
     * SUCCESS / FAILED
     */
    private String status;

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