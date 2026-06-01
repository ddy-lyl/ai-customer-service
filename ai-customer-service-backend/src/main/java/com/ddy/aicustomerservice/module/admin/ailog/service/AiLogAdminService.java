package com.ddy.aicustomerservice.module.admin.ailog.service;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:10
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiChatSessionPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiRetrievalLogPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiToolCallLogPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiChatMessageAdminVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiChatSessionAdminVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiConversationTraceVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiRetrievalLogVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiToolCallLogVO;

import java.util.List;

/**
 * AI日志后台管理业务接口
 */
public interface AiLogAdminService {

    /**
     * 分页查询AI会话
     */
    PageResult<AiChatSessionAdminVO> pageSessions(AiChatSessionPageQuery query);

    /**
     * 查询会话消息列表
     */
    List<AiChatMessageAdminVO> listSessionMessages(Long sessionId);

    /**
     * 查询会话完整排查链路
     */
    AiConversationTraceVO getSessionTrace(Long sessionId);

    /**
     * 分页查询知识召回日志
     */
    PageResult<AiRetrievalLogVO> pageRetrievalLogs(AiRetrievalLogPageQuery query);

    /**
     * 查询知识召回日志详情
     */
    AiRetrievalLogVO getRetrievalLogDetail(Long id);

    /**
     * 分页查询工具调用日志
     */
    PageResult<AiToolCallLogVO> pageToolCallLogs(AiToolCallLogPageQuery query);

    /**
     * 查询工具调用日志详情
     */
    AiToolCallLogVO getToolCallLogDetail(Long id);
}