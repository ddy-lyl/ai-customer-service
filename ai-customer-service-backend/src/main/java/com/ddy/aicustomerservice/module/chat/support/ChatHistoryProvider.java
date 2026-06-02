package com.ddy.aicustomerservice.module.chat.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddy.aicustomerservice.common.enums.ChatMessageRoleEnum;
import com.ddy.aicustomerservice.config.AiCustomerServiceProperties;
import com.ddy.aicustomerservice.module.chat.entity.ChatMessage;
import com.ddy.aicustomerservice.module.chat.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从 MySQL chat_message 加载多轮对话历史，供 ChatClient 注入上下文。
 *
 * 仅包含 USER / ASSISTANT，不含 STAFF / SYSTEM / TOOL；
 * RAG 片段由当前轮 userPrompt 单独携带，不写入历史消息。
 */
@Component
@RequiredArgsConstructor
public class ChatHistoryProvider {

    private static final int DEFAULT_MAX_PER_MESSAGE_CHARS = 2000;

    private final ChatMessageMapper chatMessageMapper;

    /**
     * @param sessionId           会话 ID
     * @param excludeMessageId    当前轮用户消息 ID（尚未有助手回复），避免重复注入
     * @param chatConfig          历史开关与条数/字数上限
     */
    public List<Message> loadForPrompt(Long sessionId,
                                       Long excludeMessageId,
                                       AiCustomerServiceProperties.Chat chatConfig) {
        if (sessionId == null || chatConfig == null || !Boolean.TRUE.equals(chatConfig.getHistoryEnabled())) {
            return List.of();
        }

        int maxMessages = chatConfig.getMaxHistoryMessages() != null && chatConfig.getMaxHistoryMessages() > 0
                ? chatConfig.getMaxHistoryMessages()
                : 20;
        int maxTotalChars = chatConfig.getMaxHistoryChars() != null && chatConfig.getMaxHistoryChars() > 0
                ? chatConfig.getMaxHistoryChars()
                : 6000;

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        wrapper.in(ChatMessage::getRole,
                ChatMessageRoleEnum.USER.getCode(),
                ChatMessageRoleEnum.ASSISTANT.getCode());
        if (excludeMessageId != null && excludeMessageId > 0) {
            wrapper.lt(ChatMessage::getId, excludeMessageId);
        }
        wrapper.orderByDesc(ChatMessage::getId);
        wrapper.last("LIMIT " + maxMessages);

        List<ChatMessage> rows = chatMessageMapper.selectList(wrapper);
        if (rows.isEmpty()) {
            return List.of();
        }

        Collections.reverse(rows);

        List<Message> messages = new ArrayList<>(rows.size());
        int totalChars = 0;

        // 从最新往旧选，保证超限时保留最近几轮
        for (int i = rows.size() - 1; i >= 0; i--) {
            ChatMessage row = rows.get(i);
            String content = trimMessageContent(row.getContent());
            if (!StringUtils.hasText(content)) {
                continue;
            }

            Message message = toSpringAiMessage(row.getRole(), content);
            if (message == null) {
                continue;
            }

            int messageChars = content.length();
            if (totalChars + messageChars > maxTotalChars) {
                break;
            }

            messages.add(0, message);
            totalChars += messageChars;
        }

        return messages;
    }

    private static String trimMessageContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= DEFAULT_MAX_PER_MESSAGE_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, DEFAULT_MAX_PER_MESSAGE_CHARS);
    }

    private static Message toSpringAiMessage(String role, String content) {
        if (ChatMessageRoleEnum.USER.getCode().equals(role)) {
            return new UserMessage(content);
        }
        if (ChatMessageRoleEnum.ASSISTANT.getCode().equals(role)) {
            return new AssistantMessage(content);
        }
        return null;
    }
}
