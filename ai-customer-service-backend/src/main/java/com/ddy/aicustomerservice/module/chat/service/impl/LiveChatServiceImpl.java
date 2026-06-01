package com.ddy.aicustomerservice.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.enums.ChatIntentEnum;
import com.ddy.aicustomerservice.common.enums.ChatMessageRoleEnum;
import com.ddy.aicustomerservice.common.enums.ChatServiceModeEnum;
import com.ddy.aicustomerservice.common.enums.ChatSessionStatusEnum;
import com.ddy.aicustomerservice.common.enums.HandoffReasonEnum;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.common.enums.TicketStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageQuery;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.chat.dto.LiveChatAcceptRequest;
import com.ddy.aicustomerservice.module.chat.dto.LiveChatSendRequest;
import com.ddy.aicustomerservice.module.chat.websocket.LiveChatPushService;
import com.ddy.aicustomerservice.module.chat.entity.ChatMessage;
import com.ddy.aicustomerservice.module.chat.entity.ChatSession;
import com.ddy.aicustomerservice.module.chat.mapper.ChatMessageMapper;
import com.ddy.aicustomerservice.module.chat.mapper.ChatSessionMapper;
import com.ddy.aicustomerservice.module.chat.service.LiveChatService;
import com.ddy.aicustomerservice.module.chat.support.HandoffTriggerEvaluator;
import com.ddy.aicustomerservice.module.chat.support.RoleChatResolver;
import com.ddy.aicustomerservice.module.chat.vo.ChatMessageVO;
import com.ddy.aicustomerservice.module.chat.vo.ChatSessionDetailVO;
import com.ddy.aicustomerservice.module.chat.vo.HandoffResultVO;
import com.ddy.aicustomerservice.module.chat.vo.LiveChatSessionVO;
import com.ddy.aicustomerservice.module.retrieval.vo.KnowledgeRetrievalResultVO;
import com.ddy.aicustomerservice.module.ticket.entity.Ticket;
import com.ddy.aicustomerservice.module.ticket.mapper.TicketMapper;
import com.ddy.aicustomerservice.module.ticket.service.TicketService;
import com.ddy.aicustomerservice.module.ticket.vo.TicketVO;
import com.ddy.aicustomerservice.module.user.entity.SysUser;
import com.ddy.aicustomerservice.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在线人工客服实现
 */
@Service
@RequiredArgsConstructor
public class LiveChatServiceImpl implements LiveChatService {

    private static final String INTENT_HANDOFF = "HANDOFF";

    private final ChatSessionMapper chatSessionMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final TicketMapper ticketMapper;

    private final TicketService ticketService;

    private final SysUserMapper sysUserMapper;

    private final HandoffTriggerEvaluator handoffTriggerEvaluator;

    private final LiveChatPushService liveChatPushService;

    @Override
    public boolean isLiveMode(ChatSession session) {
        String mode = resolveServiceMode(session);
        return ChatServiceModeEnum.WAITING_AGENT.getCode().equals(mode)
                || ChatServiceModeEnum.HUMAN.getCode().equals(mode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HandoffResultVO requestHandoff(Long sessionId) {
        ChatSession session = getSessionForCurrentUser(sessionId);
        if (!handoffEnabledForCurrentUser()) {
            return buildHandoffResult(session, false, null, null);
        }
        if (isLiveMode(session)) {
            return buildHandoffResult(session, false, null, null);
        }
        return doHandoff(session, HandoffReasonEnum.USER_REQUEST);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HandoffResultVO evaluateAfterAiRound(ChatSession session,
                                                 String questionText,
                                                 KnowledgeRetrievalResultVO retrieval,
                                                 String answer) {
        if (!handoffEnabledForCurrentUser()) {
            return buildHandoffResult(session, false, null, null);
        }
        if (isLiveMode(session)) {
            return buildHandoffResult(session, false, null, null);
        }

        int recentMiss = countRecentRagMiss(session.getId());
        HandoffReasonEnum reason = handoffTriggerEvaluator.evaluate(
                questionText,
                retrieval,
                answer,
                recentMiss
        );
        if (reason == null) {
            HandoffResultVO vo = new HandoffResultVO();
            vo.setHandoffTriggered(false);
            vo.setServiceMode(resolveServiceMode(session));
            return vo;
        }
        return doHandoff(session, reason);
    }

    @Override
    public ChatSessionDetailVO getSessionDetail(Long sessionId) {
        ChatSession session = getSessionReadable(sessionId);
        return toSessionDetailVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendUserLiveMessage(Long sessionId, LiveChatSendRequest request) {
        Long userId = LoginUserContext.getRequired().getUserId();
        ChatSession session = getSessionForCurrentUser(sessionId);
        if (!isLiveMode(session)) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "当前会话未处于人工接待，请继续与 AI 对话或点击转人工"
            );
        }

        ChatMessage message = saveMessage(
                session,
                ChatMessageRoleEnum.USER.getCode(),
                userId,
                null,
                request.getContent().trim(),
                null
        );
        touchSession(session.getId());

        if (ChatServiceModeEnum.WAITING_AGENT.getCode().equals(resolveServiceMode(session))
                && handoffTriggerEvaluator.matchesUserRequest(request.getContent())) {
            // 已在排队，无需重复转接
        }

        ChatMessageVO vo = toMessageVO(message, Map.of());
        liveChatPushService.pushMessage(session.getId(), vo);
        return vo;
    }

    @Override
    public List<ChatMessageVO> listMessages(Long sessionId, Long afterMessageId) {
        ChatSession session = getSessionForCurrentUser(sessionId);
        return listMessagesInternal(session, afterMessageId);
    }

    @Override
    public PageResult<LiveChatSessionVO> pageWaitingSessions(PageQuery query) {
        requireStaff();

        Page<ChatSession> page = new Page<>(query.getSafePageNo(), query.getSafePageSize());
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getServiceMode, ChatServiceModeEnum.WAITING_AGENT.getCode());
        wrapper.eq(ChatSession::getStatus, ChatSessionStatusEnum.ACTIVE.getCode());
        wrapper.orderByAsc(ChatSession::getHandoffTime);
        wrapper.orderByDesc(ChatSession::getLastMessageTime);

        Page<ChatSession> result = chatSessionMapper.selectPage(page, wrapper);
        return PageResult.of(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                toLiveSessionVOList(result.getRecords())
        );
    }

    @Override
    public PageResult<LiveChatSessionVO> pageMyLiveSessions(PageQuery query) {
        Long staffId = LoginUserContext.getRequired().getUserId();
        requireStaff();

        Page<ChatSession> page = new Page<>(query.getSafePageNo(), query.getSafePageSize());
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getServiceMode, ChatServiceModeEnum.HUMAN.getCode());
        wrapper.eq(ChatSession::getAssignedStaffId, staffId);
        wrapper.eq(ChatSession::getStatus, ChatSessionStatusEnum.ACTIVE.getCode());
        wrapper.orderByDesc(ChatSession::getLastMessageTime);

        Page<ChatSession> result = chatSessionMapper.selectPage(page, wrapper);
        return PageResult.of(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                toLiveSessionVOList(result.getRecords())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptSession(Long sessionId, LiveChatAcceptRequest request) {
        LoginUserInfo staff = LoginUserContext.getRequired();
        requireStaffRole(staff);

        ChatSession session = getSessionRequired(sessionId);
        if (!ChatServiceModeEnum.WAITING_AGENT.getCode().equals(resolveServiceMode(session))) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "该会话不在等待接待状态"
            );
        }

        Integer expectedVersion = request.getExpectedVersion();
        Integer currentVersion = session.getVersion() != null ? session.getVersion() : 0;
        if (!expectedVersion.equals(currentVersion)) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "会话已被其他客服接入或状态已变更，请刷新后重试"
            );
        }

        session.setServiceMode(ChatServiceModeEnum.HUMAN.getCode());
        session.setAssignedStaffId(staff.getUserId());
        int updated = chatSessionMapper.updateById(session);
        if (updated == 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "接入失败，会话可能已被其他客服抢占"
            );
        }

        if (session.getActiveTicketId() != null) {
            try {
                ticketService.claimTicket(session.getActiveTicketId());
            } catch (BusinessException ex) {
                // 工单可能已被认领，忽略
            }
        }

        String staffName = StringUtils.hasText(staff.getNickname())
                ? staff.getNickname()
                : staff.getUsername();
        ChatMessage sysMsg = saveMessage(
                session,
                ChatMessageRoleEnum.SYSTEM.getCode(),
                session.getUserId(),
                null,
                "客服 " + staffName + " 已接入，正在为您服务。",
                INTENT_HANDOFF
        );
        touchSession(sessionId);

        ChatSession refreshed = getSessionRequired(sessionId);
        ChatMessageVO msgVo = toMessageVO(sysMsg, Map.of(staff.getUserId(), staffName));
        liveChatPushService.pushMessage(sessionId, msgVo);
        liveChatPushService.pushSessionUpdate(sessionId, toSessionDetailVO(refreshed));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void endHumanSession(Long sessionId) {
        LoginUserInfo staff = LoginUserContext.getRequired();
        requireStaffRole(staff);

        ChatSession session = getSessionRequired(sessionId);
        if (!ChatServiceModeEnum.HUMAN.getCode().equals(resolveServiceMode(session))) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "会话未处于人工接待中");
        }
        if (!staff.getUserId().equals(session.getAssignedStaffId())
                && !staff.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "仅当前接待客服可结束会话");
        }

        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setServiceMode(ChatServiceModeEnum.AI.getCode());
        update.setAssignedStaffId(null);
        chatSessionMapper.updateById(update);

        saveMessage(
                session,
                ChatMessageRoleEnum.SYSTEM.getCode(),
                session.getUserId(),
                null,
                "人工客服已结束接待，您可继续向 AI 提问。",
                INTENT_HANDOFF
        );
        touchSession(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendStaffMessage(Long sessionId, LiveChatSendRequest request) {
        LoginUserInfo staff = LoginUserContext.getRequired();
        requireStaffRole(staff);

        ChatSession session = getSessionRequired(sessionId);
        if (!ChatServiceModeEnum.HUMAN.getCode().equals(resolveServiceMode(session))) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "请先接入会话再回复用户");
        }
        if (!staff.getUserId().equals(session.getAssignedStaffId())
                && !staff.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "您不是该会话的接待客服");
        }

        ChatMessage message = saveMessage(
                session,
                ChatMessageRoleEnum.STAFF.getCode(),
                session.getUserId(),
                staff.getUserId(),
                request.getContent().trim(),
                null
        );
        touchSession(sessionId);
        Map<Long, String> names = Map.of(staff.getUserId(), resolveNickname(staff));
        ChatMessageVO vo = toMessageVO(message, names);
        liveChatPushService.pushMessage(sessionId, vo);
        return vo;
    }

    @Override
    public List<ChatMessageVO> listStaffMessages(Long sessionId, Long afterMessageId) {
        requireStaff();
        ChatSession session = getSessionRequired(sessionId);
        if (!staffCanView(session)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "无权查看该会话");
        }
        return listMessagesInternal(session, afterMessageId);
    }

    private HandoffResultVO doHandoff(ChatSession session, HandoffReasonEnum reason) {
        ChatSession fresh = getSessionRequired(session.getId());
        if (isLiveMode(fresh)) {
            return buildHandoffResult(fresh, false, null, null);
        }

        TicketVO ticket = ensureHandoffTicket(fresh, reason);
        LocalDateTime now = LocalDateTime.now();

        ChatSession update = new ChatSession();
        update.setId(fresh.getId());
        update.setServiceMode(ChatServiceModeEnum.WAITING_AGENT.getCode());
        update.setActiveTicketId(ticket.getId());
        update.setHandoffReason(reason.getCode());
        update.setHandoffTime(now);
        chatSessionMapper.updateById(update);

        fresh = getSessionRequired(fresh.getId());
        String notice = "已为您转接人工客服，请稍候。关联工单：" + ticket.getTicketNo()
                + "。客服接入后将在本窗口直接回复您。";
        ChatMessage systemMsg = saveMessage(
                fresh,
                ChatMessageRoleEnum.SYSTEM.getCode(),
                fresh.getUserId(),
                null,
                notice,
                INTENT_HANDOFF
        );
        touchSession(fresh.getId());

        ChatSession refreshed = getSessionRequired(fresh.getId());
        liveChatPushService.pushMessage(
                refreshed.getId(),
                toMessageVO(systemMsg, Map.of())
        );
        liveChatPushService.pushSessionUpdate(refreshed.getId(), toSessionDetailVO(refreshed));

        return buildHandoffResult(refreshed, true, reason, systemMsg);
    }

    private TicketVO ensureHandoffTicket(ChatSession session, HandoffReasonEnum reason) {
        if (session.getActiveTicketId() != null) {
            Ticket existing = ticketMapper.selectById(session.getActiveTicketId());
            if (existing != null
                    && !TicketStatusEnum.CLOSED.getCode().equals(existing.getStatus())
                    && !TicketStatusEnum.CANCELLED.getCode().equals(existing.getStatus())) {
                TicketVO vo = new TicketVO();
                vo.setId(existing.getId());
                vo.setTicketNo(existing.getTicketNo());
                return vo;
            }
        }
        String summary = buildConversationSummary(session.getId());
        return ticketService.createHandoffTicket(
                session.getId(),
                session.getUserId(),
                reason.getCode(),
                summary
        );
    }

    private String buildConversationSummary(Long sessionId) {
        List<ChatMessage> recent = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .in(ChatMessage::getRole,
                                ChatMessageRoleEnum.USER.getCode(),
                                ChatMessageRoleEnum.ASSISTANT.getCode())
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT 6")
        );
        if (recent.isEmpty()) {
            return "用户请求转人工客服";
        }
        StringBuilder sb = new StringBuilder("近期对话摘要：\n");
        for (int i = recent.size() - 1; i >= 0; i--) {
            ChatMessage msg = recent.get(i);
            String role = ChatMessageRoleEnum.USER.getCode().equals(msg.getRole()) ? "用户" : "AI";
            String line = msg.getContent();
            if (line != null && line.length() > 200) {
                line = line.substring(0, 200) + "...";
            }
            sb.append("- ").append(role).append("：").append(line).append("\n");
        }
        return sb.toString();
    }

    private int countRecentRagMiss(Long sessionId) {
        List<ChatMessage> userMsgs = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getRole, ChatMessageRoleEnum.USER.getCode())
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT 3")
        );
        if (userMsgs.isEmpty()) {
            return 0;
        }
        int miss = 0;
        for (ChatMessage userMsg : userMsgs) {
            ChatMessage assistant = chatMessageMapper.selectOne(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getSessionId, sessionId)
                            .eq(ChatMessage::getRole, ChatMessageRoleEnum.ASSISTANT.getCode())
                            .gt(ChatMessage::getId, userMsg.getId())
                            .orderByAsc(ChatMessage::getId)
                            .last("LIMIT 1")
            );
            if (assistant != null
                    && ChatIntentEnum.GENERAL_CHAT.getCode().equals(assistant.getIntent())) {
                miss++;
            }
        }
        return miss;
    }

    private List<ChatMessageVO> listMessagesInternal(ChatSession session, Long afterMessageId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, session.getId());
        if (afterMessageId != null && afterMessageId > 0) {
            wrapper.gt(ChatMessage::getId, afterMessageId);
        }
        wrapper.orderByAsc(ChatMessage::getCreateTime);
        wrapper.orderByAsc(ChatMessage::getId);

        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        Map<Long, String> senderNames = resolveSenderNames(messages);
        return messages.stream()
                .map(m -> toMessageVO(m, senderNames))
                .toList();
    }

    private Map<Long, String> resolveSenderNames(List<ChatMessage> messages) {
        Set<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(senderIds);
        Map<Long, String> map = new HashMap<>();
        for (SysUser user : users) {
            map.put(user.getId(), StringUtils.hasText(user.getNickname())
                    ? user.getNickname()
                    : user.getUsername());
        }
        return map;
    }

    private ChatMessage saveMessage(ChatSession session,
                                    String role,
                                    Long ownerUserId,
                                    Long senderId,
                                    String content,
                                    String intent) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setUserId(ownerUserId);
        message.setRole(role);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setIntent(intent);
        chatMessageMapper.insert(message);
        return message;
    }

    private void touchSession(Long sessionId) {
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setLastMessageTime(LocalDateTime.now());
        chatSessionMapper.updateById(update);
    }

    /**
     * 转人工仅面向 C 端消费者；客服/管理员使用 AI 助手时不进入排队或人工接待。
     */
    private boolean handoffEnabledForCurrentUser() {
        return RoleChatResolver.isEndUser(LoginUserContext.get());
    }

    private HandoffResultVO buildHandoffResult(ChatSession session,
                                               boolean triggered,
                                               HandoffReasonEnum reason,
                                               ChatMessage systemMsg) {
        HandoffResultVO vo = new HandoffResultVO();
        vo.setHandoffTriggered(triggered);
        String mode = resolveServiceMode(session);
        vo.setServiceMode(mode);
        vo.setServiceModeName(resolveServiceModeName(mode));
        if (reason != null) {
            vo.setHandoffReason(reason.getCode());
            vo.setHandoffReasonName(reason.getName());
        }
        if (session.getActiveTicketId() != null) {
            Ticket ticket = ticketMapper.selectById(session.getActiveTicketId());
            if (ticket != null) {
                vo.setTicketId(ticket.getId());
                vo.setTicketNo(ticket.getTicketNo());
            }
        }
        if (systemMsg != null) {
            vo.setSystemMessageId(systemMsg.getId());
            vo.setNotice(systemMsg.getContent());
        }
        return vo;
    }

    private List<LiveChatSessionVO> toLiveSessionVOList(List<ChatSession> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = sessions.stream().map(ChatSession::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        return sessions.stream().map(s -> {
            LiveChatSessionVO vo = new LiveChatSessionVO();
            vo.setSessionId(s.getId());
            vo.setUserId(s.getUserId());
            SysUser u = userMap.get(s.getUserId());
            if (u != null) {
                vo.setUserNickname(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
            }
            vo.setTitle(s.getTitle());
            String mode = resolveServiceMode(s);
            vo.setServiceMode(mode);
            vo.setServiceModeName(resolveServiceModeName(mode));
            vo.setAssignedStaffId(s.getAssignedStaffId());
            vo.setLastMessageTime(s.getLastMessageTime());
            vo.setHandoffTime(s.getHandoffTime());
            vo.setActiveTicketId(s.getActiveTicketId());
            if (s.getActiveTicketId() != null) {
                Ticket t = ticketMapper.selectById(s.getActiveTicketId());
                if (t != null) {
                    vo.setActiveTicketNo(t.getTicketNo());
                }
            }
            vo.setLastUserMessage(findLastUserMessagePreview(s.getId()));
            vo.setVersion(s.getVersion() != null ? s.getVersion() : 0);
            return vo;
        }).toList();
    }

    private String findLastUserMessagePreview(Long sessionId) {
        ChatMessage msg = chatMessageMapper.selectOne(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getRole, ChatMessageRoleEnum.USER.getCode())
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT 1")
        );
        if (msg == null || !StringUtils.hasText(msg.getContent())) {
            return "";
        }
        String c = msg.getContent().trim();
        return c.length() > 60 ? c.substring(0, 60) + "..." : c;
    }

    private ChatSessionDetailVO toSessionDetailVO(ChatSession session) {
        ChatSessionDetailVO vo = new ChatSessionDetailVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setTitle(session.getTitle());
        vo.setStatus(session.getStatus());
        vo.setStatusName(ChatSessionStatusEnum.ACTIVE.getCode().equals(session.getStatus())
                ? ChatSessionStatusEnum.ACTIVE.getName()
                : ChatSessionStatusEnum.CLOSED.getName());
        String mode = resolveServiceMode(session);
        vo.setServiceMode(mode);
        vo.setServiceModeName(resolveServiceModeName(mode));
        vo.setAssignedStaffId(session.getAssignedStaffId());
        if (session.getAssignedStaffId() != null) {
            SysUser staff = sysUserMapper.selectById(session.getAssignedStaffId());
            if (staff != null) {
                vo.setAssignedStaffName(resolveNickname(staff));
            }
        }
        vo.setActiveTicketId(session.getActiveTicketId());
        if (session.getActiveTicketId() != null) {
            Ticket t = ticketMapper.selectById(session.getActiveTicketId());
            if (t != null) {
                vo.setActiveTicketNo(t.getTicketNo());
            }
        }
        vo.setHandoffReason(session.getHandoffReason());
        if (StringUtils.hasText(session.getHandoffReason())) {
            for (HandoffReasonEnum r : HandoffReasonEnum.values()) {
                if (r.getCode().equals(session.getHandoffReason())) {
                    vo.setHandoffReasonName(r.getName());
                    break;
                }
            }
        }
        vo.setHandoffTime(session.getHandoffTime());
        vo.setLastMessageTime(session.getLastMessageTime());
        vo.setVersion(session.getVersion() != null ? session.getVersion() : 0);
        return vo;
    }

    private ChatMessageVO toMessageVO(ChatMessage message, Map<Long, String> senderNames) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setUserId(message.getUserId());
        vo.setRole(message.getRole());
        vo.setRoleName(convertRoleName(message.getRole()));
        vo.setSenderId(message.getSenderId());
        if (message.getSenderId() != null) {
            vo.setSenderName(senderNames.get(message.getSenderId()));
        }
        vo.setContent(message.getContent());
        vo.setIntent(message.getIntent());
        vo.setModelName(message.getModelName());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private String convertRoleName(String role) {
        for (ChatMessageRoleEnum item : ChatMessageRoleEnum.values()) {
            if (item.getCode().equals(role)) {
                return item.getName();
            }
        }
        return "未知";
    }

    private String resolveServiceMode(ChatSession session) {
        if (session == null || !StringUtils.hasText(session.getServiceMode())) {
            return ChatServiceModeEnum.AI.getCode();
        }
        return session.getServiceMode();
    }

    private String resolveServiceModeName(String mode) {
        for (ChatServiceModeEnum item : ChatServiceModeEnum.values()) {
            if (item.getCode().equals(mode)) {
                return item.getName();
            }
        }
        return mode;
    }

    private ChatSession getSessionForCurrentUser(Long sessionId) {
        ChatSession session = getSessionRequired(sessionId);
        Long currentUserId = LoginUserContext.getRequired().getUserId();
        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "没有权限操作该会话");
        }
        if (ChatSessionStatusEnum.CLOSED.getCode().equals(session.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "会话已关闭");
        }
        return session;
    }

    private ChatSession getSessionReadable(Long sessionId) {
        ChatSession session = getSessionRequired(sessionId);
        LoginUserInfo user = LoginUserContext.getRequired();
        if (user.getUserId().equals(session.getUserId())) {
            return session;
        }
        if (staffCanView(session)) {
            return session;
        }
        throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "没有权限查看该会话");
    }

    private boolean staffCanView(ChatSession session) {
        LoginUserInfo user = LoginUserContext.getRequired();
        if (user.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            return true;
        }
        if (!user.hasRole(RoleCodeEnum.STAFF.getCode())) {
            return false;
        }
        if (ChatServiceModeEnum.WAITING_AGENT.getCode().equals(resolveServiceMode(session))) {
            return true;
        }
        return user.getUserId().equals(session.getAssignedStaffId());
    }

    private ChatSession getSessionRequired(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "会话不存在");
        }
        return session;
    }

    private void requireStaff() {
        LoginUserInfo user = LoginUserContext.getRequired();
        requireStaffRole(user);
    }

    private void requireStaffRole(LoginUserInfo user) {
        if (!user.hasRole(RoleCodeEnum.STAFF.getCode())
                && !user.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "仅客服可访问");
        }
    }

    private String resolveNickname(LoginUserInfo user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private String resolveNickname(SysUser user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }
}
