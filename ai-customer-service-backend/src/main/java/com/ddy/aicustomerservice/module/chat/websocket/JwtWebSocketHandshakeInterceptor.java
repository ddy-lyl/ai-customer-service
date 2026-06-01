package com.ddy.aicustomerservice.module.chat.websocket;

import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.security.UserTokenVersionService;
import com.ddy.aicustomerservice.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手 JWT 校验，token 通过 query 参数 {@code token=} 传入。
 */
@Component
@RequiredArgsConstructor
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_LOGIN_USER = "loginUser";

    private final JwtUtils jwtUtils;

    private final UserTokenVersionService userTokenVersionService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        if (!StringUtils.hasText(token) || !jwtUtils.isTokenValid(token)) {
            return false;
        }
        try {
            String tokenVersion = jwtUtils.getTokenVersion(token);
            if (!StringUtils.hasText(tokenVersion)) {
                return false;
            }
            LoginUserInfo loginUser = jwtUtils.parseLoginUser(token);
            if (!userTokenVersionService.isTokenVersionValid(loginUser.getUserId(), tokenVersion)) {
                return false;
            }
            attributes.put(ATTR_LOGIN_USER, loginUser);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
