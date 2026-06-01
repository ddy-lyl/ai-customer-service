package com.ddy.aicustomerservice.config;

import com.ddy.aicustomerservice.module.chat.websocket.JwtWebSocketHandshakeInterceptor;
import com.ddy.aicustomerservice.module.chat.websocket.LiveChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 在线客服 WebSocket 端点：{@code /ws/live-chat?token=...}
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class LiveChatWebSocketConfig implements WebSocketConfigurer {

    private final LiveChatWebSocketHandler liveChatWebSocketHandler;

    private final JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveChatWebSocketHandler, "/ws/live-chat")
                .addInterceptors(jwtWebSocketHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
