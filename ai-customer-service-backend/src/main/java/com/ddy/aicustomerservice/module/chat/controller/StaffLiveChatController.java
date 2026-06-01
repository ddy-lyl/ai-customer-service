package com.ddy.aicustomerservice.module.chat.controller;

import com.ddy.aicustomerservice.common.model.PageQuery;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.chat.dto.LiveChatAcceptRequest;
import com.ddy.aicustomerservice.module.chat.dto.LiveChatSendRequest;
import com.ddy.aicustomerservice.module.chat.service.LiveChatService;
import com.ddy.aicustomerservice.module.chat.vo.ChatMessageVO;
import com.ddy.aicustomerservice.module.chat.vo.ChatSessionDetailVO;
import com.ddy.aicustomerservice.module.chat.vo.LiveChatSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客服在线接待
 */
@RestController
@RequestMapping("/api/staff/live-chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffLiveChatController {

    private final LiveChatService liveChatService;

    @GetMapping("/sessions/waiting")
    public Result<PageResult<LiveChatSessionVO>> pageWaiting(PageQuery query) {
        return Result.success(liveChatService.pageWaitingSessions(query));
    }

    @GetMapping("/sessions/mine")
    public Result<PageResult<LiveChatSessionVO>> pageMine(PageQuery query) {
        return Result.success(liveChatService.pageMyLiveSessions(query));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionDetailVO> sessionDetail(@PathVariable Long sessionId) {
        return Result.success(liveChatService.getSessionDetail(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/accept")
    public Result<Void> accept(@PathVariable Long sessionId,
                               @Valid @RequestBody LiveChatAcceptRequest request) {
        liveChatService.acceptSession(sessionId, request);
        return Result.success();
    }

    @PostMapping("/sessions/{sessionId}/end")
    public Result<Void> end(@PathVariable Long sessionId) {
        liveChatService.endHumanSession(sessionId);
        return Result.success();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> messages(@PathVariable Long sessionId,
                                                @RequestParam(required = false) Long afterMessageId) {
        return Result.success(liveChatService.listStaffMessages(sessionId, afterMessageId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public Result<ChatMessageVO> send(@PathVariable Long sessionId,
                                      @Valid @RequestBody LiveChatSendRequest request) {
        return Result.success(liveChatService.sendStaffMessage(sessionId, request));
    }
}
