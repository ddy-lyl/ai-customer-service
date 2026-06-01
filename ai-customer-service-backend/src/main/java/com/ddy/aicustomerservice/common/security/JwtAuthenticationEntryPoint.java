package com.ddy.aicustomerservice.common.security;

/**
 * @author 罗亚兰
 * @date 2026/5/15 23:13
 **/

import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未登录处理器
 *
 * 当用户未登录或 Token 无效时，会进入这里。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Result<Void> result = Result.fail(
                ResultCodeEnum.UNAUTHORIZED.getCode(),
                "未登录或登录已过期"
        );

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}