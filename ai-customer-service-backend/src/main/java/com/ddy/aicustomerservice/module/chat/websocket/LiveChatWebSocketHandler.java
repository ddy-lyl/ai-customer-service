package com.ddy.aicustomerservice.module.chat.websocket;

import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 在线客服 WebSocket：客户端发送 {@code {"action":"subscribe","sessionId":123}} 订阅会话推送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveChatWebSocketHandler extends TextWebSocketHandler {

    public static final String ATTR_CHAT_SESSION_ID = "chatSessionId";

    private final LiveChatWebSocketSessionRegistry registry;

    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        LoginUserInfo user = (LoginUserInfo) session.getAttributes()
                .get(JwtWebSocketHandshakeInterceptor.ATTR_LOGIN_USER);
        if (user == null) {
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String action = root.path("action").asText("");
            if (!"subscribe".equals(action)) {
                return;
            }
            long sessionId = root.path("sessionId").asLong(0);
            if (sessionId <= 0) {
                return;
            }
            Long previous = (Long) session.getAttributes().get(ATTR_CHAT_SESSION_ID);
            if (previous != null) {
                registry.unsubscribe(previous, session);
            }
            session.getAttributes().put(ATTR_CHAT_SESSION_ID, sessionId);
            registry.subscribe(sessionId, session);
        } catch (Exception e) {
            log.debug("[LiveChat WS] 无法解析客户端消息: {}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object sid = session.getAttributes().get(ATTR_CHAT_SESSION_ID);
        if (sid instanceof Long sessionId) {
            registry.unsubscribe(sessionId, session);
        } else {
            registry.remove(session);
        }
    }
}
