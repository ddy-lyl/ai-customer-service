package com.ddy.aicustomerservice.module.admin.ailog.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:10
 **/

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.constant.AiToolConstants;
import com.ddy.aicustomerservice.common.enums.ChatMessageRoleEnum;
import com.ddy.aicustomerservice.common.enums.ChatSessionStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiChatSessionPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiRetrievalLogPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.dto.AiToolCallLogPageQuery;
import com.ddy.aicustomerservice.module.admin.ailog.service.AiLogAdminService;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiChatMessageAdminVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiChatSessionAdminVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiConversationTraceVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiRetrievalLogVO;
import com.ddy.aicustomerservice.module.admin.ailog.vo.AiToolCallLogVO;
import com.ddy.aicustomerservice.module.ai.tool.entity.AiToolCallLog;
import com.ddy.aicustomerservice.module.ai.tool.mapper.AiToolCallLogMapper;
import com.ddy.aicustomerservice.module.chat.entity.ChatMessage;
import com.ddy.aicustomerservice.module.chat.entity.ChatSession;
import com.ddy.aicustomerservice.module.chat.mapper.ChatMessageMapper;
import com.ddy.aicustomerservice.module.chat.mapper.ChatSessionMapper;
import com.ddy.aicustomerservice.module.retrieval.entity.AiRetrievalLog;
import com.ddy.aicustomerservice.module.retrieval.mapper.AiRetrievalLogMapper;
import com.ddy.aicustomerservice.module.user.entity.SysUser;
import com.ddy.aicustomerservice.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * AI日志后台管理业务实现类
 */
@Service
@RequiredArgsConstructor
public class AiLogAdminServiceImpl implements AiLogAdminService {

    private final ChatSessionMapper chatSessionMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final AiRetrievalLogMapper aiRetrievalLogMapper;

    private final AiToolCallLogMapper aiToolCallLogMapper;

    private final SysUserMapper sysUserMapper;

    /**
     * 分页查询AI会话
     */
    @Override
    public PageResult<AiChatSessionAdminVO> pageSessions(AiChatSessionPageQuery query) {
        Page<ChatSession> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(query.getUserId() != null,
                ChatSession::getUserId,
                query.getUserId());

        wrapper.like(StringUtils.hasText(query.getTitleKeyword()),
                ChatSession::getTitle,
                query.getTitleKeyword());

        wrapper.eq(StringUtils.hasText(query.getStatus()),
                ChatSession::getStatus,
                query.getStatus());

        wrapper.ge(query.getStartTime() != null,
                ChatSession::getCreateTime,
                query.getStartTime());

        wrapper.le(query.getEndTime() != null,
                ChatSession::getCreateTime,
                query.getEndTime());

        wrapper.orderByDesc(ChatSession::getLastMessageTime);
        wrapper.orderByDesc(ChatSession::getCreateTime);

        Page<ChatSession> sessionPage = chatSessionMapper.selectPage(page, wrapper);

        List<AiChatSessionAdminVO> records = sessionPage.getRecords()
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

    /**
     * 查询会话消息列表
     */
    @Override
    public List<AiChatMessageAdminVO> listSessionMessages(Long sessionId) {
        getSessionRequired(sessionId);

        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime)
                        .orderByAsc(ChatMessage::getId)
        );

        return messages.stream()
                .map(this::convertMessageVO)
                .toList();
    }

    /**
     * 查询会话完整排查链路
     */
    @Override
    public AiConversationTraceVO getSessionTrace(Long sessionId) {
        ChatSession session = getSessionRequired(sessionId);

        AiConversationTraceVO vo = new AiConversationTraceVO();

        vo.setSession(convertSessionVO(session));
        vo.setMessages(listSessionMessages(sessionId));
        vo.setRetrievalLogs(listRetrievalLogsBySessionId(sessionId));
        vo.setToolCallLogs(listToolCallLogsBySessionId(sessionId));

        return vo;
    }

    /**
     * 分页查询知识召回日志
     */
    @Override
    public PageResult<AiRetrievalLogVO> pageRetrievalLogs(AiRetrievalLogPageQuery query) {
        Page<AiRetrievalLog> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<AiRetrievalLog> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(query.getSessionId() != null,
                AiRetrievalLog::getSessionId,
                query.getSessionId());

        wrapper.eq(query.getUserId() != null,
                AiRetrievalLog::getUserId,
                query.getUserId());

        wrapper.eq(query.getUserMessageId() != null,
                AiRetrievalLog::getUserMessageId,
                query.getUserMessageId());

        wrapper.like(StringUtils.hasText(query.getQuestionKeyword()),
                AiRetrievalLog::getQuestionText,
                query.getQuestionKeyword());

        wrapper.eq(query.getKnowledgeBaseId() != null,
                AiRetrievalLog::getKnowledgeBaseId,
                query.getKnowledgeBaseId());

        wrapper.eq(query.getDocumentId() != null,
                AiRetrievalLog::getDocumentId,
                query.getDocumentId());

        wrapper.eq(query.getChunkId() != null,
                AiRetrievalLog::getChunkId,
                query.getChunkId());

        wrapper.ge(query.getMinScore() != null,
                AiRetrievalLog::getScore,
                query.getMinScore());

        wrapper.le(query.getMaxScore() != null,
                AiRetrievalLog::getScore,
                query.getMaxScore());

        wrapper.ge(query.getStartTime() != null,
                AiRetrievalLog::getCreateTime,
                query.getStartTime());

        wrapper.le(query.getEndTime() != null,
                AiRetrievalLog::getCreateTime,
                query.getEndTime());

        wrapper.orderByDesc(AiRetrievalLog::getCreateTime);
        wrapper.orderByAsc(AiRetrievalLog::getRankNo);

        Page<AiRetrievalLog> logPage = aiRetrievalLogMapper.selectPage(page, wrapper);

        List<AiRetrievalLogVO> records = logPage.getRecords()
                .stream()
                .map(this::convertRetrievalLogVO)
                .toList();

        return PageResult.of(
                logPage.getCurrent(),
                logPage.getSize(),
                logPage.getTotal(),
                logPage.getPages(),
                records
        );
    }

    /**
     * 查询知识召回日志详情
     */
    @Override
    public AiRetrievalLogVO getRetrievalLogDetail(Long id) {
        AiRetrievalLog log = aiRetrievalLogMapper.selectById(id);

        if (log == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "知识召回日志不存在"
            );
        }

        return convertRetrievalLogVO(log);
    }

    /**
     * 分页查询工具调用日志
     */
    @Override
    public PageResult<AiToolCallLogVO> pageToolCallLogs(AiToolCallLogPageQuery query) {
        Page<AiToolCallLog> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<AiToolCallLog> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(query.getSessionId() != null,
                AiToolCallLog::getSessionId,
                query.getSessionId());

        wrapper.eq(query.getUserId() != null,
                AiToolCallLog::getUserId,
                query.getUserId());

        wrapper.eq(query.getUserMessageId() != null,
                AiToolCallLog::getUserMessageId,
                query.getUserMessageId());

        wrapper.eq(StringUtils.hasText(query.getToolName()),
                AiToolCallLog::getToolName,
                query.getToolName());

        wrapper.eq(StringUtils.hasText(query.getStatus()),
                AiToolCallLog::getStatus,
                query.getStatus());

        wrapper.ge(query.getStartTime() != null,
                AiToolCallLog::getCreateTime,
                query.getStartTime());

        wrapper.le(query.getEndTime() != null,
                AiToolCallLog::getCreateTime,
                query.getEndTime());

        wrapper.orderByDesc(AiToolCallLog::getCreateTime);

        Page<AiToolCallLog> logPage = aiToolCallLogMapper.selectPage(page, wrapper);

        List<AiToolCallLogVO> records = logPage.getRecords()
                .stream()
                .map(this::convertToolCallLogVO)
                .toList();

        return PageResult.of(
                logPage.getCurrent(),
                logPage.getSize(),
                logPage.getTotal(),
                logPage.getPages(),
                records
        );
    }

    /**
     * 查询工具调用日志详情
     */
    @Override
    public AiToolCallLogVO getToolCallLogDetail(Long id) {
        AiToolCallLog log = aiToolCallLogMapper.selectById(id);

        if (log == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "工具调用日志不存在"
            );
        }

        return convertToolCallLogVO(log);
    }

    /**
     * 根据会话ID查询知识召回日志
     */
    private List<AiRetrievalLogVO> listRetrievalLogsBySessionId(Long sessionId) {
        List<AiRetrievalLog> logs = aiRetrievalLogMapper.selectList(
                new LambdaQueryWrapper<AiRetrievalLog>()
                        .eq(AiRetrievalLog::getSessionId, sessionId)
                        .orderByAsc(AiRetrievalLog::getUserMessageId)
                        .orderByAsc(AiRetrievalLog::getRankNo)
                        .orderByAsc(AiRetrievalLog::getCreateTime)
        );

        return logs.stream()
                .map(this::convertRetrievalLogVO)
                .toList();
    }

    /**
     * 根据会话ID查询工具调用日志
     */
    private List<AiToolCallLogVO> listToolCallLogsBySessionId(Long sessionId) {
        List<AiToolCallLog> logs = aiToolCallLogMapper.selectList(
                new LambdaQueryWrapper<AiToolCallLog>()
                        .eq(AiToolCallLog::getSessionId, sessionId)
                        .orderByAsc(AiToolCallLog::getUserMessageId)
                        .orderByAsc(AiToolCallLog::getCreateTime)
        );

        return logs.stream()
                .map(this::convertToolCallLogVO)
                .toList();
    }

    /**
     * 查询会话，不存在则抛异常
     */
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

    /**
     * 会话 Entity 转 VO
     */
    private AiChatSessionAdminVO convertSessionVO(ChatSession session) {
        AiChatSessionAdminVO vo = new AiChatSessionAdminVO();

        SysUser user = getUser(session.getUserId());

        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setTitle(session.getTitle());
        vo.setStatus(session.getStatus());
        vo.setStatusName(convertSessionStatusName(session.getStatus()));
        vo.setLastMessageTime(session.getLastMessageTime());
        vo.setMessageCount(countMessages(session.getId()));
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());

        return vo;
    }

    /**
     * 消息 Entity 转 VO
     */
    private AiChatMessageAdminVO convertMessageVO(ChatMessage message) {
        AiChatMessageAdminVO vo = new AiChatMessageAdminVO();

        SysUser user = getUser(message.getUserId());

        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setUserId(message.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setRole(message.getRole());
        vo.setRoleName(convertMessageRoleName(message.getRole()));
        vo.setContent(message.getContent());
        vo.setContentPreview(buildPreview(message.getContent(), 120));
        vo.setCreateTime(message.getCreateTime());

        return vo;
    }

    /**
     * 知识召回日志 Entity 转 VO
     */
    private AiRetrievalLogVO convertRetrievalLogVO(AiRetrievalLog log) {
        AiRetrievalLogVO vo = new AiRetrievalLogVO();

        SysUser user = getUser(log.getUserId());

        vo.setId(log.getId());
        vo.setSessionId(log.getSessionId());
        vo.setUserId(log.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setUserMessageId(log.getUserMessageId());
        vo.setQuestionText(log.getQuestionText());
        vo.setQuestionPreview(buildPreview(log.getQuestionText(), 120));
        vo.setKnowledgeBaseId(log.getKnowledgeBaseId());
        vo.setDocumentId(log.getDocumentId());
        vo.setDocumentName(log.getDocumentName());
        vo.setChunkId(log.getChunkId());
        vo.setChunkContent(log.getChunkContent());
        vo.setChunkContentPreview(buildPreview(log.getChunkContent(), 160));
        vo.setScore(log.getScore());
        vo.setRankNo(log.getRankNo());
        vo.setCreateTime(log.getCreateTime());
        vo.setUpdateTime(log.getUpdateTime());

        return vo;
    }

    /**
     * 工具调用日志 Entity 转 VO
     */
    private AiToolCallLogVO convertToolCallLogVO(AiToolCallLog log) {
        AiToolCallLogVO vo = new AiToolCallLogVO();

        SysUser user = getUser(log.getUserId());

        vo.setId(log.getId());
        vo.setSessionId(log.getSessionId());
        vo.setUserId(log.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setUserMessageId(log.getUserMessageId());
        vo.setToolName(log.getToolName());
        vo.setToolNameText(convertToolNameText(log.getToolName()));
        vo.setRequestJson(log.getRequestJson());
        vo.setRequestPreview(buildPreview(log.getRequestJson(), 160));
        vo.setResponseJson(log.getResponseJson());
        vo.setResponsePreview(buildPreview(log.getResponseJson(), 160));
        vo.setStatus(log.getStatus());
        vo.setStatusName(convertToolStatusName(log.getStatus()));
        vo.setErrorMessage(log.getErrorMessage());
        vo.setCostMillis(log.getCostMillis());
        vo.setCreateTime(log.getCreateTime());
        vo.setUpdateTime(log.getUpdateTime());

        return vo;
    }

    /**
     * 查询用户
     */
    private SysUser getUser(Long userId) {
        if (userId == null) {
            return null;
        }

        return sysUserMapper.selectById(userId);
    }

    /**
     * 统计会话消息数量
     */
    private Long countMessages(Long sessionId) {
        return chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
        );
    }

    /**
     * 构建预览文本
     */
    private String buildPreview(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        String cleaned = text
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        return cleaned.substring(0, maxLength) + "...";
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
        if (ChatMessageRoleEnum.USER.getCode().equals(role)) {
            return ChatMessageRoleEnum.USER.getName();
        }

        if (ChatMessageRoleEnum.ASSISTANT.getCode().equals(role)) {
            return ChatMessageRoleEnum.ASSISTANT.getName();
        }

        if (ChatMessageRoleEnum.SYSTEM.getCode().equals(role)) {
            return ChatMessageRoleEnum.SYSTEM.getName();
        }

        return "未知角色";
    }

    private String convertToolNameText(String toolName) {
        if (AiToolConstants.TOOL_QUERY_ORDER.equals(toolName)) {
            return "查询订单";
        }

        if (AiToolConstants.TOOL_LIST_MY_ORDERS.equals(toolName)) {
            return "列出我的订单";
        }

        if (AiToolConstants.TOOL_CREATE_TICKET.equals(toolName)) {
            return "创建工单";
        }

        if (AiToolConstants.TOOL_QUERY_TICKET_STATUS.equals(toolName)) {
            return "查询工单状态";
        }

        return toolName;
    }

    private String convertToolStatusName(String status) {
        if (AiToolConstants.STATUS_SUCCESS.equals(status)) {
            return "调用成功";
        }

        if (AiToolConstants.STATUS_FAILED.equals(status)) {
            return "调用失败";
        }

        return "未知状态";
    }
}