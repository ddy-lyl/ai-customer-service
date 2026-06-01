package com.ddy.aicustomerservice.common.context;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:34
 **/

import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;

/**
 * 登录用户上下文
 *
 * 使用 ThreadLocal 保存当前请求的用户信息。
 *
 * 注意：
 * 每次请求结束后必须 remove，
 * 否则 Tomcat 线程复用时可能出现用户信息串号。
 */
public class LoginUserContext {

    private static final ThreadLocal<LoginUserInfo> USER_CONTEXT = new ThreadLocal<>();

    private LoginUserContext() {
    }

    /**
     * 设置当前登录用户
     */
    public static void set(LoginUserInfo loginUserInfo) {
        USER_CONTEXT.set(loginUserInfo);
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUserInfo get() {
        return USER_CONTEXT.get();
    }

    /**
     * 获取当前登录用户，如果未登录则抛异常
     */
    public static LoginUserInfo getRequired() {
        LoginUserInfo loginUserInfo = get();

        if (loginUserInfo == null) {
            throw new BusinessException(
                    ResultCodeEnum.UNAUTHORIZED.getCode(),
                    "请先登录"
            );
        }

        return loginUserInfo;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId() {
        return getRequired().getUserId();
    }

    /**
     * 获取当前登录用户名
     */
    public static String getUsername() {
        return getRequired().getUsername();
    }

    /**
     * 判断当前用户是否拥有某个角色
     */
    public static boolean hasRole(String roleCode) {
        return getRequired().hasRole(roleCode);
    }

    /**
     * 清理当前线程中的用户信息
     */
    public static void remove() {
        USER_CONTEXT.remove();
    }
}
