package com.ddy.aicustomerservice.module.order.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/17 13:54
 **/
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.order.service.OrderService;
import com.ddy.aicustomerservice.module.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 普通用户订单控制器
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 查询我的订单列表
     *
     * 仅普通用户（C 端）使用；客服/管理员请走 staff/admin 订单接口。
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public Result<List<OrderVO>> listMyOrders() {
        List<OrderVO> orders = orderService.listMyOrders();
        return Result.success(orders);
    }

    /**
     * 根据订单号查询我的订单详情
     *
     * 示例：
     * GET /api/orders/202605150001
     */
    @GetMapping("/{orderNo}")
    @PreAuthorize("hasRole('USER')")
    public Result<OrderVO> getMyOrderByOrderNo(@PathVariable String orderNo) {
        OrderVO order = orderService.getMyOrderByOrderNo(orderNo);
        return Result.success(order);
    }
}
