package com.ddy.aicustomerservice.module.chat.support;

import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;

/**
 * 根据当前登录用户解析 AI 对话主角色（USER / STAFF / ADMIN）。
 */
public final class RoleChatResolver {

    private RoleChatResolver() {
    }

    public static RoleCodeEnum resolvePrimaryRole() {
        LoginUserInfo user = LoginUserContext.get();
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return RoleCodeEnum.USER;
        }
        if (user.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            return RoleCodeEnum.ADMIN;
        }
        if (user.hasRole(RoleCodeEnum.STAFF.getCode())) {
            return RoleCodeEnum.STAFF;
        }
        return RoleCodeEnum.USER;
    }

    public static boolean isEndUser(LoginUserInfo user) {
        return user != null
                && user.hasRole(RoleCodeEnum.USER.getCode())
                && !user.hasRole(RoleCodeEnum.STAFF.getCode())
                && !user.hasRole(RoleCodeEnum.ADMIN.getCode());
    }
}
