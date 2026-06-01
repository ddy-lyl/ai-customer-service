package com.ddy.aicustomerservice.module.auth.controller;

import com.ddy.aicustomerservice.common.constant.SecurityConstants;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.auth.dto.LoginRequest;
import com.ddy.aicustomerservice.module.auth.dto.RegisterRequest;
import com.ddy.aicustomerservice.module.auth.service.AuthService;
import com.ddy.aicustomerservice.module.auth.vo.CurrentUserVO;
import com.ddy.aicustomerservice.module.auth.vo.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * 提供：
 * 1. POST /api/auth/register   注册（默认绑定 USER 角色）
 * 2. POST /api/auth/login      登录，签发 JWT
 * 3. GET  /api/auth/me         查询当前登录用户
 * 4. POST /api/auth/logout     退出登录，将当前 Token 加入 Redis 黑名单
 */
@Tag(name = "01.认证模块", description = "注册、登录、退出登录、获取当前用户")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<CurrentUserVO> me() {
        return Result.success(authService.getCurrentUser());
    }

    @Operation(summary = "退出登录（将当前 Token 加入 Redis 黑名单）")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = SecurityConstants.AUTHORIZATION_HEADER, required = false)
            String authorizationHeader
    ) {
        authService.logout(authorizationHeader);
        return Result.success();
    }
}
