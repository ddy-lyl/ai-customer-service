package com.ddy.aicustomerservice.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.constant.AiToolConstants;
import com.ddy.aicustomerservice.common.util.ChineseTextNormalizer;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.enums.ChatIntentEnum;
import com.ddy.aicustomerservice.common.enums.ChatMessageRoleEnum;
import com.ddy.aicustomerservice.common.enums.ChatServiceModeEnum;
import com.ddy.aicustomerservice.common.enums.ChatSessionStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageQuery;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.config.AiCustomerServiceProperties;
import com.ddy.aicustomerservice.module.ai.tool.context.AiToolCallContext;
import com.ddy.aicustomerservice.module.ai.tool.context.AiToolCallContextInfo;
import com.ddy.aicustomerservice.module.ai.tool.entity.AiToolCallLog;
import com.ddy.aicustomerservice.module.ai.tool.mapper.AiToolCallLogMapper;
import com.ddy.aicustomerservice.module.ai.tool.tools.AiAdminAssistTools;
import com.ddy.aicustomerservice.module.ai.tool.tools.AiBusinessTools;
import com.ddy.aicustomerservice.module.ai.tool.tools.AiStaffAssistTools;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.module.chat.support.RoleChatResolver;
import com.ddy.aicustomerservice.module.chat.dto.KnowledgeQaRequest;
import com.ddy.aicustomerservice.module.chat.dto.LiveChatSendRequest;
import com.ddy.aicustomerservice.module.chat.service.LiveChatService;
import com.ddy.aicustomerservice.module.chat.vo.HandoffResultVO;
import com.ddy.aicustomerservice.module.chat.entity.ChatMessage;
import com.ddy.aicustomerservice.module.chat.entity.ChatSession;
import com.ddy.aicustomerservice.module.chat.mapper.ChatMessageMapper;
import com.ddy.aicustomerservice.module.chat.mapper.ChatSessionMapper;
import com.ddy.aicustomerservice.module.chat.service.AiKnowledgeChatService;
import com.ddy.aicustomerservice.module.chat.support.ChatHistoryProvider;
import com.ddy.aicustomerservice.module.chat.support.KnowledgePromptBuilder;
import com.ddy.aicustomerservice.module.chat.vo.ChatMessageVO;
import com.ddy.aicustomerservice.module.chat.vo.ChatSessionVO;
import com.ddy.aicustomerservice.module.chat.vo.KnowledgeQaResponseVO;
import com.ddy.aicustomerservice.module.chat.vo.KnowledgeQaStreamDoneVO;
import com.ddy.aicustomerservice.module.chat.vo.KnowledgeQaStreamMetaVO;
import com.ddy.aicustomerservice.module.chat.vo.KnowledgeSourceVO;
import com.ddy.aicustomerservice.module.retrieval.dto.KnowledgeRetrievalRequest;
import com.ddy.aicustomerservice.module.retrieval.service.KnowledgeRetrievalService;
import com.ddy.aicustomerservice.module.retrieval.vo.KnowledgeRetrievalResultVO;
import com.ddy.aicustomerservice.module.retrieval.vo.RetrievedChunkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 知识库问答业务实现类
 *
 * 一次问答流程：
 * 1. 解析/创建会话（独立事务）
 * 2. 落库用户消息（独立事务）
 * 3. 知识库检索 + 工具调用 + AI 回答（不持有数据库事务，避免长事务）
 * 4. 落库 AI 回复 + 回填意图/模型名/会话最后消息时间（独立事务）
 *
 * 拆事务的原因：
 * 大模型 API 调用是一次较长的 HTTP 阻塞调用，
 * 如果整个流程包在 @Transactional 里，会长时间占用数据库连接，
 * 并发上来很容易把连接池打满。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeChatServiceImpl implements AiKnowledgeChatService {

    private final ChatSessionMapper chatSessionMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    private final ChatClient.Builder chatClientBuilder;

    private final AiBusinessTools aiBusinessTools;

    private final AiStaffAssistTools aiStaffAssistTools;

    private final AiAdminAssistTools aiAdminAssistTools;

    private final AiToolCallLogMapper aiToolCallLogMapper;

    private final AiCustomerServiceProperties properties;

    private final LiveChatService liveChatService;

    private final ChatHistoryProvider chatHistoryProvider;

    /**
     * 当前实际使用的对话模型名称
     *
     * 直接从 spring.ai 配置读取，避免代码里再硬编码。
     */
    @Value("${spring.ai.deepseek.chat.options.model:unknown}")
    private String chatModelName;

    @Override
    public KnowledgeQaResponseVO ask(KnowledgeQaRequest request) {
        Long currentUserId = LoginUserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(
                    ResultCodeEnum.UNAUTHORIZED.getCode(),
                    "请先登录"
            );
        }

        ChatSession session = resolveSession(
                request.getSessionId(),
                currentUserId,
                request.getQuestionText()
        );

        if (liveChatService.isLiveMode(session)) {
            return askInLiveMode(session, request);
        }

        ChatMessage userMessage = saveUserMessage(
                session.getId(),
                currentUserId,
                request.getQuestionText()
        );

        KnowledgeRetrievalResultVO retrievalResult =
                doRetrieve(session.getId(), userMessage.getId(), request);

        String answer = generateAnswer(
                session.getId(),
                currentUserId,
                userMessage.getId(),
                request.getQuestionText(),
                retrievalResult
        );

        String intent = inferIntent(userMessage.getId(), retrievalResult);

        ChatMessage assistantMessage = saveAssistantMessage(
                session.getId(),
                currentUserId,
                answer,
                intent
        );

        backfillUserMessageIntent(userMessage, intent);

        touchSessionLastMessageTime(session);

        session = getSessionRequired(session.getId());
        HandoffResultVO handoff = liveChatService.evaluateAfterAiRound(
                session,
                request.getQuestionText(),
                retrievalResult,
                answer
        );
        String finalAnswer = mergeAnswerWithHandoff(answer, handoff);

        KnowledgeQaResponseVO response = new KnowledgeQaResponseVO();
        response.setSessionId(session.getId());
        response.setUserMessageId(userMessage.getId());
        response.setAssistantMessageId(assistantMessage.getId());
        response.setQuestionText(request.getQuestionText());
        response.setAnswer(finalAnswer);
        response.setRetrievedCount(retrievalResult.getRetrievedCount());
        response.setSources(convertSources(retrievalResult.getChunks()));
        response.setIntent(intent);
        response.setModelName(chatModelName);
        applyHandoffToResponse(response, handoff, getSessionRequired(session.getId()));
        return response;
    }

    private KnowledgeQaResponseVO askInLiveMode(ChatSession session, KnowledgeQaRequest request) {
        LiveChatSendRequest liveReq = new LiveChatSendRequest();
        liveReq.setContent(request.getQuestionText());
        ChatMessageVO userMsg = liveChatService.sendUserLiveMessage(session.getId(), liveReq);

        KnowledgeQaResponseVO response = new KnowledgeQaResponseVO();
        response.setSessionId(session.getId());
        response.setUserMessageId(userMsg.getId());
        response.setQuestionText(request.getQuestionText());
        response.setAnswer(buildLiveModeHint(session));
        response.setRetrievedCount(0);
        response.setSources(List.of());
        response.setServiceMode(resolveServiceMode(session));
        return response;
    }

    private String buildLiveModeHint(ChatSession session) {
        String mode = resolveServiceMode(session);
        if (ChatServiceModeEnum.HUMAN.getCode().equals(mode)) {
            return "您的消息已发送给在线客服，请在本窗口查看回复。";
        }
        return "您的消息已发送，正在排队等待客服接入，请稍候。";
    }

    /**
     * SSE 流式问答超时时间：5 分钟
     *
     * 云端模型生成可能较慢，给足上限避免 SSE 连接被提前关闭。
     */
    private static final long SSE_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    @Override
    public SseEmitter askStream(KnowledgeQaRequest request) {
        Long currentUserId = LoginUserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(
                    ResultCodeEnum.UNAUTHORIZED.getCode(),
                    "请先登录"
            );
        }

        ChatSession session = resolveSession(
                request.getSessionId(),
                currentUserId,
                request.getQuestionText()
        );

        if (liveChatService.isLiveMode(session)) {
            return askStreamInLiveMode(session, request);
        }

        ChatMessage userMessage = saveUserMessage(
                session.getId(),
                currentUserId,
                request.getQuestionText()
        );

        KnowledgeRetrievalResultVO retrievalResult =
                doRetrieve(session.getId(), userMessage.getId(), request);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        KnowledgeQaStreamMetaVO meta = new KnowledgeQaStreamMetaVO();
        meta.setSessionId(session.getId());
        meta.setUserMessageId(userMessage.getId());
        meta.setQuestionText(request.getQuestionText());
        meta.setRetrievedCount(retrievalResult.getRetrievedCount());
        meta.setSources(convertSources(retrievalResult.getChunks()));
        if (!sendEvent(emitter, "meta", meta)) {
            return emitter;
        }

        Flux<String> tokenFlux = buildTokenFlux(
                session.getId(),
                userMessage.getId(),
                request.getQuestionText(),
                retrievalResult
        );

        StringBuilder fullAnswer = new StringBuilder();

        Disposable subscription = tokenFlux.subscribe(
                token -> handleStreamToken(emitter, fullAnswer, token),
                error -> handleStreamError(emitter, session.getId(), currentUserId, error),
                () -> handleStreamComplete(
                        emitter,
                        session,
                        currentUserId,
                        userMessage,
                        retrievalResult,
                        fullAnswer
                )
        );

        emitter.onTimeout(() -> {
            log.warn("[AI 流式问答] 超时, sessionId={}", session.getId());
            subscription.dispose();
            emitter.complete();
        });
        emitter.onError(t -> subscription.dispose());
        emitter.onCompletion(subscription::dispose);

        return emitter;
    }

    /**
     * 构造给 Spring AI 的 token 流
     *
     * Plan B：流式问答不挂载业务工具，避免工具上下文跨线程丢失。
     * 涉及订单/工单的请求建议走同步 ask 接口。
     */
    private Flux<String> buildTokenFlux(Long sessionId,
                                        Long currentUserMessageId,
                                        String questionText,
                                        KnowledgeRetrievalResultVO retrievalResult) {
        String contextText = retrievalResult == null ? "" : retrievalResult.getContextText();
        RoleCodeEnum chatRole = RoleChatResolver.resolvePrimaryRole();
        String systemPrompt = KnowledgePromptBuilder.resolveSystemPromptForRole(
                chatRole,
                properties.getChat()
        );
        String userPrompt = KnowledgePromptBuilder.buildUserPrompt(chatRole, questionText, contextText);

        var prompt = chatClientBuilder.build().prompt().system(systemPrompt);
        applyChatHistory(prompt, sessionId, currentUserMessageId);
        return prompt.user(userPrompt).stream().content();
    }

    private void handleStreamToken(SseEmitter emitter,
                                   StringBuilder fullAnswer,
                                   String token) {
        if (token == null) {
            return;
        }
        String normalized = ChineseTextNormalizer.toSimplified(token);
        fullAnswer.append(normalized);
        sendEvent(emitter, "delta", Map.of("content", normalized));
    }

    private void handleStreamError(SseEmitter emitter,
                                   Long sessionId,
                                   Long userId,
                                   Throwable error) {
        log.error("[AI 流式问答失败] sessionId={} userId={}", sessionId, userId, error);
        sendEvent(emitter, "error",
                Map.of("message", "AI 服务暂时不可用，请稍后再试，或直接发起人工工单。"));
        emitter.complete();
    }

    private void handleStreamComplete(SseEmitter emitter,
                                      ChatSession session,
                                      Long currentUserId,
                                      ChatMessage userMessage,
                                      KnowledgeRetrievalResultVO retrievalResult,
                                      StringBuilder fullAnswer) {
        try {
            String finalAnswer = fullAnswer.toString().trim();
            if (!StringUtils.hasText(finalAnswer)) {
                finalAnswer = "当前 AI 模型没有生成有效回答，请稍后重试或联系人工客服。";
            }

            String intent = (retrievalResult != null
                    && retrievalResult.getRetrievedCount() != null
                    && retrievalResult.getRetrievedCount() > 0)
                    ? ChatIntentEnum.KNOWLEDGE_QA.getCode()
                    : ChatIntentEnum.GENERAL_CHAT.getCode();

            ChatMessage assistantMessage = saveAssistantMessage(
                    session.getId(),
                    currentUserId,
                    finalAnswer,
                    intent
            );
            backfillUserMessageIntent(userMessage, intent);
            touchSessionLastMessageTime(session);

            ChatSession freshSession = getSessionRequired(session.getId());
            HandoffResultVO handoff = liveChatService.evaluateAfterAiRound(
                    freshSession,
                    userMessage.getContent(),
                    retrievalResult,
                    finalAnswer
            );
            String mergedAnswer = mergeAnswerWithHandoff(finalAnswer, handoff);

            KnowledgeQaStreamDoneVO done = new KnowledgeQaStreamDoneVO();
            done.setSessionId(session.getId());
            done.setAssistantMessageId(assistantMessage.getId());
            done.setFullAnswer(mergedAnswer);
            done.setIntent(intent);
            done.setModelName(chatModelName);
            applyHandoffToStreamDone(done, handoff, getSessionRequired(session.getId()));

            sendEvent(emitter, "done", done);
        } catch (Exception ex) {
            log.error("[AI 流式问答收尾失败] sessionId={} userId={}",
                    session.getId(), currentUserId, ex);
            sendEvent(emitter, "error",
                    Map.of("message", "AI 回复保存失败，请稍后再试。"));
        } finally {
            emitter.complete();
        }
    }

    /**
     * 安全推送 SSE 事件
     *
     * 网络中断或客户端断开时，emitter.send 会抛 IOException，
     * 这里只记日志，避免影响其它推送链路。
     */
    private boolean sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.warn("[AI 流式问答] 推送事件失败, event={}, reason={}",
                    eventName, e.getMessage());
            return false;
        }
    }

    @Override
    public PageResult<ChatSessionVO> pageMySessions(PageQuery query) {
        Long currentUserId = LoginUserContext.getUserId();

        Page<ChatSession> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, currentUserId);
        wrapper.orderByDesc(ChatSession::getLastMessageTime);
        wrapper.orderByDesc(ChatSession::getCreateTime);

        Page<ChatSession> sessionPage = chatSessionMapper.selectPage(page, wrapper);

        List<ChatSessionVO> records = sessionPage.getRecords()
                .stream()
                .map(this::convertSessionVO)
                .toList();

        return PageResult.of(
                sessionPage.getCurrent(),
                sessionPage.getSize(),
                sessionPage.getTotal(),
                sessionPage.getPages(),
                records
        );
    }

    @Override
    public List<ChatMessageVO> listMessages(Long sessionId, Long afterMessageId) {
        return liveChatService.listMessages(sessionId, afterMessageId);
    }

    private SseEmitter askStreamInLiveMode(ChatSession session, KnowledgeQaRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        LiveChatSendRequest liveReq = new LiveChatSendRequest();
        liveReq.setContent(request.getQuestionText());
        ChatMessageVO userMsg = liveChatService.sendUserLiveMessage(session.getId(), liveReq);

        KnowledgeQaStreamMetaVO meta = new KnowledgeQaStreamMetaVO();
        meta.setSessionId(session.getId());
        meta.setUserMessageId(userMsg.getId());
        meta.setQuestionText(request.getQuestionText());
        meta.setRetrievedCount(0);
        meta.setSources(List.of());
        sendEvent(emitter, "meta", meta);

        String hint = buildLiveModeHint(getSessionRequired(session.getId()));
        sendEvent(emitter, "delta", Map.of("content", hint));

        KnowledgeQaStreamDoneVO done = new KnowledgeQaStreamDoneVO();
        done.setSessionId(session.getId());
        done.setFullAnswer(hint);
        done.setIntent(ChatIntentEnum.GENERAL_CHAT.getCode());
        done.setServiceMode(resolveServiceMode(session));
        sendEvent(emitter, "done", done);
        emitter.complete();
        return emitter;
    }

    private String mergeAnswerWithHandoff(String answer, HandoffResultVO handoff) {
        if (handoff == null || !Boolean.TRUE.equals(handoff.getHandoffTriggered())) {
            return answer;
        }
        if (!StringUtils.hasText(handoff.getNotice())) {
            return answer;
        }
        return answer + "\n\n---\n\n" + handoff.getNotice();
    }

    private void applyHandoffToResponse(KnowledgeQaResponseVO response,
                                        HandoffResultVO handoff,
                                        ChatSession session) {
        if (handoff == null) {
            return;
        }
        response.setHandoffTriggered(handoff.getHandoffTriggered());
        response.setServiceMode(resolveServiceMode(session));
        response.setHandoffNotice(handoff.getNotice());
        response.setHandoffTicketId(handoff.getTicketId());
        response.setHandoffTicketNo(handoff.getTicketNo());
    }

    private void applyHandoffToStreamDone(KnowledgeQaStreamDoneVO done,
                                          HandoffResultVO handoff,
                                          ChatSession session) {
        if (handoff == null) {
            return;
        }
        done.setHandoffTriggered(handoff.getHandoffTriggered());
        done.setServiceMode(resolveServiceMode(session));
        done.setHandoffNotice(handoff.getNotice());
        done.setHandoffTicketId(handoff.getTicketId());
        done.setHandoffTicketNo(handoff.getTicketNo());
    }

    private String resolveServiceMode(ChatSession session) {
        if (session == null || !StringUtils.hasText(session.getServiceMode())) {
            return ChatServiceModeEnum.AI.getCode();
        }
        return session.getServiceMode();
    }

    private ChatSession resolveSession(Long sessionId,
                                       Long currentUserId,
                                       String questionText) {
        if (sessionId == null) {
            return createSession(currentUserId, questionText);
        }

        ChatSession session = getSessionRequired(sessionId);

        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN.getCode(),
                    "没有权限使用该会话"
            );
        }
        if (ChatSessionStatusEnum.CLOSED.getCode().equals(session.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "会话已关闭，不能继续提问"
            );
        }
        return session;
    }

    private ChatSession createSession(Long userId, String questionText) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(generateSessionTitle(questionText));
        session.setStatus(ChatSessionStatusEnum.ACTIVE.getCode());
        session.setServiceMode(ChatServiceModeEnum.AI.getCode());
        session.setLastMessageTime(LocalDateTime.now());

        chatSessionMapper.insert(session);
        return session;
    }

    private ChatMessage saveUserMessage(Long sessionId, Long userId, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(ChatMessageRoleEnum.USER.getCode());
        message.setContent(content);
        chatMessageMapper.insert(message);
        return message;
    }

    private ChatMessage saveAssistantMessage(Long sessionId,
                                             Long userId,
                                             String content,
                                             String intent) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(ChatMessageRoleEnum.ASSISTANT.getCode());
        message.setContent(content);
        message.setIntent(intent);
        message.setModelName(chatModelName);
        chatMessageMapper.insert(message);
        return message;
    }

    private void backfillUserMessageIntent(ChatMessage userMessage, String intent) {
        ChatMessage update = new ChatMessage();
        update.setId(userMessage.getId());
        update.setIntent(intent);
        chatMessageMapper.updateById(update);
    }

    private void touchSessionLastMessageTime(ChatSession session) {
        ChatSession update = new ChatSession();
        update.setId(session.getId());
        update.setLastMessageTime(LocalDateTime.now());
        chatSessionMapper.updateById(update);
    }

    private KnowledgeRetrievalResultVO doRetrieve(Long sessionId,
                                                  Long userMessageId,
                                                  KnowledgeQaRequest request) {
        KnowledgeRetrievalRequest retrievalRequest = new KnowledgeRetrievalRequest();
        retrievalRequest.setSessionId(sessionId);
        retrievalRequest.setUserMessageId(userMessageId);
        retrievalRequest.setQuestionText(request.getQuestionText());
        retrievalRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        retrievalRequest.setTopK(request.getTopK() != null
                ? request.getTopK()
                : properties.getKnowledge().getTopK());
        retrievalRequest.setSimilarityThreshold(request.getSimilarityThreshold() != null
                ? request.getSimilarityThreshold()
                : properties.getKnowledge().getSimilarityThreshold());

        return knowledgeRetrievalService.retrieve(retrievalRequest);
    }

    private String generateAnswer(Long sessionId,
                                  Long userId,
                                  Long userMessageId,
                                  String questionText,
                                  KnowledgeRetrievalResultVO retrievalResult) {
        String contextText = retrievalResult == null ? "" : retrievalResult.getContextText();

        RoleCodeEnum chatRole = RoleChatResolver.resolvePrimaryRole();
        String systemPrompt = KnowledgePromptBuilder.resolveSystemPromptForRole(
                chatRole,
                properties.getChat()
        );
        String userPrompt = KnowledgePromptBuilder.buildUserPrompt(chatRole, questionText, contextText);
        Object[] tools = resolveToolsForRole(chatRole);

        AiToolCallContext.set(new AiToolCallContextInfo(sessionId, userId, userMessageId));

        try {
            var prompt = chatClientBuilder.build().prompt().system(systemPrompt);
            applyChatHistory(prompt, sessionId, userMessageId);
            String answer = prompt.user(userPrompt)
                    .tools(tools)
                    .call()
                    .content();

            if (!StringUtils.hasText(answer)) {
                return "当前 AI 模型没有生成有效回答，请稍后重试或联系人工客服。";
            }
            return ChineseTextNormalizer.toSimplified(answer.trim());
        } catch (Exception e) {
            log.error("[AI 对话失败] sessionId={} userId={}", sessionId, userId, e);
            return "AI 服务暂时不可用，请稍后再试，或直接发起人工工单。";
        } finally {
            AiToolCallContext.clear();
        }
    }

    /**
     * 将 MySQL 中的 USER/ASSISTANT 历史注入当前 Prompt（不含本轮用户消息）。
     */
    private void applyChatHistory(ChatClient.ChatClientRequestSpec promptSpec,
                                  Long sessionId,
                                  Long excludeMessageId) {
        List<Message> history = chatHistoryProvider.loadForPrompt(
                sessionId,
                excludeMessageId,
                properties.getChat()
        );
        if (!history.isEmpty()) {
            promptSpec.messages(history);
        }
    }

    /**
     * 根据本轮工具调用情况推断意图
     *
     * 优先级（从高到低）：
     * CREATE_TICKET > QUERY_ORDER > QUERY_TICKET_STATUS > KNOWLEDGE_QA > GENERAL_CHAT
     */
    private Object[] resolveToolsForRole(RoleCodeEnum role) {
        return switch (role) {
            case ADMIN -> new Object[]{aiBusinessTools, aiAdminAssistTools};
            case STAFF -> new Object[]{aiBusinessTools, aiStaffAssistTools};
            case USER -> new Object[]{aiBusinessTools};
        };
    }

    private String inferIntent(Long userMessageId,
                               KnowledgeRetrievalResultVO retrievalResult) {
        List<AiToolCallLog> logs = aiToolCallLogMapper.selectList(
                new LambdaQueryWrapper<AiToolCallLog>()
                        .eq(AiToolCallLog::getUserMessageId, userMessageId)
        );

        boolean hasCreateTicket = logs.stream()
                .anyMatch(l -> AiToolConstants.TOOL_CREATE_TICKET.equals(l.getToolName()));
        if (hasCreateTicket) {
            return ChatIntentEnum.TICKET_CREATE.getCode();
        }
        boolean hasQueryOrder = logs.stream()
                .anyMatch(l -> AiToolConstants.TOOL_QUERY_ORDER.equals(l.getToolName())
                        || AiToolConstants.TOOL_LIST_MY_ORDERS.equals(l.getToolName()));
        if (hasQueryOrder) {
            return ChatIntentEnum.ORDER_QUERY.getCode();
        }
        boolean hasQueryTicket = logs.stream()
                .anyMatch(l -> AiToolConstants.TOOL_QUERY_TICKET_STATUS.equals(l.getToolName()));
        if (hasQueryTicket) {
            return ChatIntentEnum.TICKET_STATUS_QUERY.getCode();
        }
        if (retrievalResult != null
                && retrievalResult.getRetrievedCount() != null
                && retrievalResult.getRetrievedCount() > 0) {
            return ChatIntentEnum.KNOWLEDGE_QA.getCode();
        }
        return ChatIntentEnum.GENERAL_CHAT.getCode();
    }

    private ChatSession getSessionRequired(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "会话不存在"
            );
        }
        return session;
    }

    private String generateSessionTitle(String questionText) {
        if (!StringUtils.hasText(questionText)) {
            return "新会话";
        }
        String title = questionText.trim();
        if (title.length() <= 20) {
            return title;
        }
        return title.substring(0, 20) + "...";
    }

    private List<KnowledgeSourceVO> convertSources(List<RetrievedChunkVO> chunks) {
        if (chunks == null) {
            return List.of();
        }
        return chunks.stream()
                .map(this::convertSource)
                .toList();
    }

    private KnowledgeSourceVO convertSource(RetrievedChunkVO chunk) {
        KnowledgeSourceVO vo = new KnowledgeSourceVO();
        vo.setRankNo(chunk.getRankNo());
        vo.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
        vo.setKnowledgeBaseName(chunk.getKnowledgeBaseName());
        vo.setDocumentId(chunk.getDocumentId());
        vo.setDocumentName(chunk.getDocumentName());
        vo.setChunkId(chunk.getChunkId());
        vo.setChunkContentPreview(chunk.getChunkContentPreview());
        vo.setScore(chunk.getScore());
        return vo;
    }

    private ChatSessionVO convertSessionVO(ChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setTitle(session.getTitle());
        vo.setStatus(session.getStatus());
        vo.setStatusName(convertSessionStatusName(session.getStatus()));
        vo.setServiceMode(resolveServiceMode(session));
        vo.setServiceModeName(convertServiceModeName(resolveServiceMode(session)));
        vo.setAssignedStaffId(session.getAssignedStaffId());
        vo.setLastMessageTime(session.getLastMessageTime());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    private ChatMessageVO convertMessageVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setUserId(message.getUserId());
        vo.setRole(message.getRole());
        vo.setRoleName(convertMessageRoleName(message.getRole()));
        vo.setContent(message.getContent());
        vo.setIntent(message.getIntent());
        vo.setModelName(message.getModelName());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private String convertServiceModeName(String mode) {
        for (ChatServiceModeEnum item : ChatServiceModeEnum.values()) {
            if (item.getCode().equals(mode)) {
                return item.getName();
            }
        }
        return mode;
    }

    private String convertSessionStatusName(String status) {
        if (ChatSessionStatusEnum.ACTIVE.getCode().equals(status)) {
            return ChatSessionStatusEnum.ACTIVE.getName();
        }
        if (ChatSessionStatusEnum.CLOSED.getCode().equals(status)) {
            return ChatSessionStatusEnum.CLOSED.getName();
        }
        return "未知状态";
    }

    private String convertMessageRoleName(String role) {
        for (ChatMessageRoleEnum item : ChatMessageRoleEnum.values()) {
            if (item.getCode().equals(role)) {
                return item.getName();
            }
        }
        return "未知角色";
    }
}
