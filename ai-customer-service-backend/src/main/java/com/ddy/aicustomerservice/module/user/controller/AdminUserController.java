package com.ddy.aicustomerservice.module.user.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/17 12:18
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.user.dto.StaffCreateRequest;
import com.ddy.aicustomerservice.module.user.dto.UserPageQuery;
import com.ddy.aicustomerservice.module.user.dto.UserPasswordResetRequest;
import com.ddy.aicustomerservice.module.user.dto.UserStatusUpdateRequest;
import com.ddy.aicustomerservice.module.user.service.UserService;
import com.ddy.aicustomerservice.module.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户管理控制器
 *
 * 路径以 /api/admin 开头。
 * SecurityConfig 中已经限制 /api/admin/** 只有 ADMIN 可以访问。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /**
     * 分页查询用户
     *
     * 示例：
     * GET /api/admin/users/page?pageNo=1&pageSize=10&username=test
     */
    @GetMapping("/page")
    public Result<PageResult<UserVO>> pageUsers(UserPageQuery query) {
        PageResult<UserVO> pageResult = userService.pageUsers(query);
        return Result.success(pageResult);
    }

    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 创建客服账号
     */
    @PostMapping("/staff")
    public Result<UserVO> createStaff(@Valid @RequestBody StaffCreateRequest request) {
        UserVO staff = userService.createStaff(request);
        return Result.success(staff);
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @Valid @RequestBody UserStatusUpdateRequest request) {
        userService.updateUserStatus(id, request);
        return Result.success();
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody UserPasswordResetRequest request) {
        userService.resetPassword(id, request);
        return Result.success();
    }

    /**
     * 查询客服列表
     *
     * 后续工单分配时会用。
     */
    @GetMapping("/staff/list")
    public Result<List<UserVO>> listStaff() {
        List<UserVO> staffList = userService.listStaff();
        return Result.success(staffList);
    }
}
