package com.ddy.aicustomerservice.module.auth.service;

/**
 * @author 罗亚兰
 * @date 2026/5/15 21:49
 **/

import com.ddy.aicustomerservice.module.auth.dto.LoginRequest;
import com.ddy.aicustomerservice.module.auth.dto.RegisterRequest;
import com.ddy.aicustomerservice.module.auth.vo.CurrentUserVO;
import com.ddy.aicustomerservice.module.auth.vo.LoginResponse;

/**
 * 认证业务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    void register(RegisterRequest request);

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     */
    CurrentUserVO getCurrentUser();

    /**
     * 退出登录
     *
     * @param authorizationHeader Authorization 请求头
     */
    void logout(String authorizationHeader);
}