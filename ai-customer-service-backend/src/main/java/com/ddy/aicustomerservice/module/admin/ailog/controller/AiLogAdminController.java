package com.ddy.aicustomerservice.module.admin.ailog.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:11
 **/

import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiChatSessionPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiRetrievalLogPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiToolCallLogPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.service.AiLogAdminService;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiChatMessageAdminVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiChatSessionAdminVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiConversationTraceVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiRetrievalLogVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiToolCallLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

/**
 * AI日志后台管理控制器
 *
 * 用于排查：
 * 1. 用户问了什么
 * 2. AI 回答了什么
 * 3. 召回了哪些知识
 * 4. 调用了哪些工具
 */
@RestController
@RequestMapping("/api/admin/ai-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiLogAdminController {

    private final AiLogAdminService aiLogAdminService;

    /**
     * 分页查询AI会话
     *
     * 示例：
     * GET /api/admin/ai-logs/sessions/page?pageNo=1&pageSize=10
     */
    @GetMapping("/sessions/page")
    public Result<PageResult<AiChatSessionAdminVO>> pageSessions(AiChatSessionPageQuery query) {
        PageResult<AiChatSessionAdminVO> pageResult = aiLogAdminService.pageSessions(query);
        return Result.success(pageResult);
    }

    /**
     * 查询某个会话的消息列表
     *
     * 示例：
     * GET /api/admin/ai-logs/sessions/12/messages
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<AiChatMessageAdminVO>> listSessionMessages(@PathVariable Long sessionId) {
        List<AiChatMessageAdminVO> messages = aiLogAdminService.listSessionMessages(sessionId);
        return Result.success(messages);
    }

    /**
     * 查询某个会话的完整排查链路
     *
     * 示例：
     * GET /api/admin/ai-logs/sessions/12/trace
     */
    @GetMapping("/sessions/{sessionId}/trace")
    public Result<AiConversationTraceVO> getSessionTrace(@PathVariable Long sessionId) {
        AiConversationTraceVO trace = aiLogAdminService.getSessionTrace(sessionId);
        return Result.success(trace);
    }

    /**
     * 分页查询知识召回日志
     *
     * 示例：
     * GET /api/admin/ai-logs/retrieval/page?pageNo=1&pageSize=10&sessionId=12
     */
    @GetMapping("/retrieval/page")
    public Result<PageResult<AiRetrievalLogVO>> pageRetrievalLogs(AiRetrievalLogPageQuery query) {
        PageResult<AiRetrievalLogVO> pageResult = aiLogAdminService.pageRetrievalLogs(query);
        return Result.success(pageResult);
    }

    /**
     * 查询知识召回日志详情
     *
     * 示例：
     * GET /api/admin/ai-logs/retrieval/1
     */
    @GetMapping("/retrieval/{id}")
    public Result<AiRetrievalLogVO> getRetrievalLogDetail(@PathVariable Long id) {
        AiRetrievalLogVO detail = aiLogAdminService.getRetrievalLogDetail(id);
        return Result.success(detail);
    }

    /**
     * 分页查询工具调用日志
     *
     * 示例：
     * GET /api/admin/ai-logs/tools/page?pageNo=1&pageSize=10
     */
    @GetMapping("/tools/page")
    public Result<PageResult<AiToolCallLogVO>> pageToolCallLogs(AiToolCallLogPageQuery query) {
        PageResult<AiToolCallLogVO> pageResult = aiLogAdminService.pageToolCallLogs(query);
        return Result.success(pageResult);
    }

    /**
     * 查询工具调用日志详情
     *
     * 示例：
     * GET /api/admin/ai-logs/tools/1
     */
    @GetMapping("/tools/{id}")
    public Result<AiToolCallLogVO> getToolCallLogDetail(@PathVariable Long id) {
        AiToolCallLogVO detail = aiLogAdminService.getToolCallLogDetail(id);
        return Result.success(detail);
    }
}
