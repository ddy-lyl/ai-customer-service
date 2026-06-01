package com.ddy.aicustomerservice.module.chat.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 按会话 ID 管理 WebSocket 连接
 */
@Component
public class LiveChatWebSocketSessionRegistry {

    private final Map<Long, Set<WebSocketSession>> sessionSubscribers = new ConcurrentHashMap<>();

    public void subscribe(Long chatSessionId, WebSocketSession wsSession) {
        sessionSubscribers
                .computeIfAbsent(chatSessionId, k -> new CopyOnWriteArraySet<>())
                .add(wsSession);
    }

    public void unsubscribe(Long chatSessionId, WebSocketSession wsSession) {
        Set<WebSocketSession> set = sessionSubscribers.get(chatSessionId);
        if (set != null) {
            set.remove(wsSession);
            if (set.isEmpty()) {
                sessionSubscribers.remove(chatSessionId);
            }
        }
    }

    public void remove(WebSocketSession wsSession) {
        sessionSubscribers.forEach((sessionId, set) -> {
            if (set.remove(wsSession) && set.isEmpty()) {
                sessionSubscribers.remove(sessionId);
            }
        });
    }

    public Set<WebSocketSession> getSubscribers(Long chatSessionId) {
        Set<WebSocketSession> set = sessionSubscribers.get(chatSessionId);
        return set == null ? Set.of() : Set.copyOf(set);
    }
}
