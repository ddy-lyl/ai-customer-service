package com.ddy.aicustomerservice.module.chat.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:58
 **/

import com.ddy.aicustomerservice.common.model.PageQuery;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.chat.dto.KnowledgeQaRequest;
import com.ddy.aicustomerservice.module.chat.dto.LiveChatSendRequest;
import com.ddy.aicustomerservice.module.chat.service.AiKnowledgeChatService;
import com.ddy.aicustomerservice.module.chat.service.LiveChatService;
import com.ddy.aicustomerservice.module.chat.vo.ChatSessionDetailVO;
import com.ddy.aicustomerservice.module.chat.vo.HandoffResultVO;
import com.ddy.aicustomerservice.module.chat.vo.ChatMessageVO;
import com.ddy.aicustomerservice.module.chat.vo.ChatSessionVO;
import com.ddy.aicustomerservice.module.chat.vo.KnowledgeQaResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI知识库问答控制器
 */
@RestController
@RequestMapping("/api/ai/knowledge-chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'STAFF', 'ADMIN')")
public class AiKnowledgeChatController {

    private final AiKnowledgeChatService aiKnowledgeChatService;

    private final LiveChatService liveChatService;

    /**
     * 知识库问答
     *
     * 示例：
     * POST /api/ai/knowledge-chat/ask
     */
    @PostMapping("/ask")
    public Result<KnowledgeQaResponseVO> ask(@Valid @RequestBody KnowledgeQaRequest request) {
        KnowledgeQaResponseVO response = aiKnowledgeChatService.ask(request);
        return Result.success(response);
    }

    /**
     * 知识库问答（SSE 流式）
     *
     * 示例：
     * POST /api/ai/knowledge-chat/ask/stream
     * Accept: text/event-stream
     *
     * 返回多帧 SSE：
     * event: meta   data: {sessionId, userMessageId, sources...}
     * event: delta  data: {content: "..."}        // 可能多帧
     * event: done   data: {assistantMessageId, fullAnswer, intent, modelName}
     * event: error  data: {message: "..."}
     *
     * 注意：当前流式接口不挂载业务工具（不会触发查订单 / 建工单），
     * 涉及工具的请求请走同步接口 POST /api/ai/knowledge-chat/ask。
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody KnowledgeQaRequest request) {
        return aiKnowledgeChatService.askStream(request);
    }

    /**
     * 分页查询我的会话
     *
     * 示例：
     * GET /api/ai/knowledge-chat/sessions/my?pageNo=1&pageSize=10
     */
    @GetMapping("/sessions/my")
    public Result<PageResult<ChatSessionVO>> pageMySessions(PageQuery query) {
        PageResult<ChatSessionVO> pageResult = aiKnowledgeChatService.pageMySessions(query);
        return Result.success(pageResult);
    }

    /**
     * 查询会话消息
     *
     * 示例：
     * GET /api/ai/knowledge-chat/sessions/1/messages
     */
    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionDetailVO> sessionDetail(@PathVariable Long sessionId) {
        return Result.success(liveChatService.getSessionDetail(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/handoff")
    @PreAuthorize("hasRole('USER')")
    public Result<HandoffResultVO> handoff(@PathVariable Long sessionId) {
        return Result.success(liveChatService.requestHandoff(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> listMessages(@PathVariable Long sessionId,
                                                    @RequestParam(required = false) Long afterMessageId) {
        List<ChatMessageVO> messages = aiKnowledgeChatService.listMessages(sessionId, afterMessageId);
        return Result.success(messages);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('USER')")
    public Result<ChatMessageVO> sendLiveMessage(@PathVariable Long sessionId,
                                                 @Valid @RequestBody LiveChatSendRequest request) {
        return Result.success(liveChatService.sendUserLiveMessage(sessionId, request));
    }
}
