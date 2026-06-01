package com.ddy.aicustomerservice.module.chat.websocket;

import com.ddy.aicustomerservice.module.chat.vo.ChatMessageVO;
import com.ddy.aicustomerservice.module.chat.vo.ChatSessionDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 在线客服 WebSocket 消息推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveChatPushService {

    private final LiveChatWebSocketSessionRegistry registry;

    private final ObjectMapper objectMapper;

    public void pushMessage(Long chatSessionId, ChatMessageVO message) {
        broadcast(chatSessionId, LiveChatWsEventType.MESSAGE.name(), message);
    }

    public void pushSessionUpdate(Long chatSessionId, ChatSessionDetailVO detail) {
        broadcast(chatSessionId, LiveChatWsEventType.SESSION_UPDATE.name(), detail);
    }

    private void broadcast(Long chatSessionId, String type, Object payload) {
        LiveChatWsMessage envelope = new LiveChatWsMessage(type, payload);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession ws : registry.getSubscribers(chatSessionId)) {
                if (ws.isOpen()) {
                    synchronized (ws) {
                        ws.sendMessage(textMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[LiveChat WS] 推送失败 sessionId={} type={}", chatSessionId, type, e);
        }
    }
}
