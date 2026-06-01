package com.ddy.aicustomerservice.module.admin.ailog.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:06
 **/

import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * AI会话分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiChatSessionPageQuery extends PageQuery {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题关键词
     */
    private String titleKeyword;

    /**
     * 会话状态：
     * ACTIVE / CLOSED
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