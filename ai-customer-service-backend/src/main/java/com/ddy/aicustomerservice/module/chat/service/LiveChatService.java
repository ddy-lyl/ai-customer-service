package com.ddy.aicustomerservice.module.chat.service;

import com.ddy.aicustomerservice.common.model.PageQuery;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.chat.dto.LiveChatAcceptRequest;
import com.ddy.aicustomerservice.module.chat.dto.LiveChatSendRequest;
import com.ddy.aicustomerservice.module.chat.entity.ChatSession;
import com.ddy.aicustomerservice.module.chat.vo.ChatMessageVO;
import com.ddy.aicustomerservice.module.chat.vo.ChatSessionDetailVO;
import com.ddy.aicustomerservice.module.chat.vo.HandoffResultVO;
import com.ddy.aicustomerservice.module.chat.vo.LiveChatSessionVO;
import com.ddy.aicustomerservice.module.retrieval.vo.KnowledgeRetrievalResultVO;

import java.util.List;

/**
 * 在线人工客服：转接、接待、双向消息
 */
public interface LiveChatService {

    boolean isLiveMode(ChatSession session);

    HandoffResultVO requestHandoff(Long sessionId);

    HandoffResultVO evaluateAfterAiRound(ChatSession session,
                                         String questionText,
                                         KnowledgeRetrievalResultVO retrieval,
                                         String answer);

    ChatSessionDetailVO getSessionDetail(Long sessionId);

    ChatMessageVO sendUserLiveMessage(Long sessionId, LiveChatSendRequest request);

    List<ChatMessageVO> listMessages(Long sessionId, Long afterMessageId);

    PageResult<LiveChatSessionVO> pageWaitingSessions(PageQuery query);

    PageResult<LiveChatSessionVO> pageMyLiveSessions(PageQuery query);

    void acceptSession(Long sessionId, LiveChatAcceptRequest request);

    void endHumanSession(Long sessionId);

    ChatMessageVO sendStaffMessage(Long sessionId, LiveChatSendRequest request);

    List<ChatMessageVO> listStaffMessages(Long sessionId, Long afterMessageId);
}
