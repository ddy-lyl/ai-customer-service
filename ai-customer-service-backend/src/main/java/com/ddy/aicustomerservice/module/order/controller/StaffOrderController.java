package com.ddy.aicustomerservice.module.order.controller;

import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.order.service.OrderService;
import com.ddy.aicustomerservice.module.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服订单控制器
 *
 * 工单处理过程中，客服经常需要核对客户订单信息。
 * 这里提供一个只读的"按订单号查"接口给客服 / 管理员，
 * 闭合"工单 -> 关联订单"链路。
 *
 * 路径在 /api/staff/** 下，由 SecurityConfig 统一限制为 STAFF + ADMIN。
 */
@Tag(name = "06.客服-订单查询", description = "客服处理工单时的订单核对接口")
@RestController
@RequestMapping("/api/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {

    private final OrderService orderService;

    @Operation(summary = "根据订单号查询订单详情（客服侧）")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> getOrderByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.getOrderByOrderNoForStaff(orderNo));
    }
}
