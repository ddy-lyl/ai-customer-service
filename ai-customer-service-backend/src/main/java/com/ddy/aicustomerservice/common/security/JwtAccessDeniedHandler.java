package com.ddy.aicustomerservice.common.security;

/**
 * @author 罗亚兰
 * @date 2026/5/15 23:15
 **/

import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 无权限处理器
 *
 * 当用户已登录，但是没有权限访问接口时，会进入这里。
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Result<Void> result = Result.fail(
                ResultCodeEnum.FORBIDDEN.getCode(),
                "没有权限访问"
        );

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
