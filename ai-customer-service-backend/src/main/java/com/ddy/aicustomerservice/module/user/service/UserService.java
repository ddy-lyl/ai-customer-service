package com.ddy.aicustomerservice.module.user.service;

/**
 * @author 罗亚兰
 * @date 2026/5/17 12:12
 **/

import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.user.dto.StaffCreateRequest;
import com.ddy.aicustomerservice.module.user.dto.UserPageQuery;
import com.ddy.aicustomerservice.module.user.dto.UserPasswordResetRequest;
import com.ddy.aicustomerservice.module.user.dto.UserStatusUpdateRequest;
import com.ddy.aicustomerservice.module.user.vo.UserVO;

import java.util.List;

/**
 * 用户业务接口
 */
public interface UserService {

    /**
     * 管理员分页查询用户
     */
    PageResult<UserVO> pageUsers(UserPageQuery query);

    /**
     * 根据用户ID查询用户详情
     */
    UserVO getUserById(Long id);

    /**
     * 管理员创建客服账号
     */
    UserVO createStaff(StaffCreateRequest request);

    /**
     * 管理员修改用户状态
     */
    void updateUserStatus(Long id, UserStatusUpdateRequest request);

    /**
     * 管理员重置用户密码
     */
    void resetPassword(Long id, UserPasswordResetRequest request);

    /**
     * 查询客服列表
     *
     * 工单分配时会用到。
     */
    List<UserVO> listStaff();

    /**
     * 当前用户查看自己的信息
     */
    UserVO getCurrentUserProfile();
}
