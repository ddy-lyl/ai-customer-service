package com.ddy.aicustomerservice.module.order.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/17 13:54
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.order.dto.OrderPageQuery;
import com.ddy.aicustomerservice.module.order.service.OrderService;
import com.ddy.aicustomerservice.module.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员订单控制器
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * 管理员分页查询订单
     *
     * 示例：
     * GET /api/admin/orders/page?pageNo=1&pageSize=10&status=RECEIVED
     */
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> pageOrders(OrderPageQuery query) {
        PageResult<OrderVO> pageResult = orderService.pageOrdersForAdmin(query);
        return Result.success(pageResult);
    }

    /**
     * 管理员根据订单号查询订单详情
     *
     * 示例：
     * GET /api/admin/orders/202605150001
     */
    @GetMapping("/{orderNo}")
    public Result<OrderVO> getOrderByOrderNo(@PathVariable String orderNo) {
        OrderVO order = orderService.getOrderByOrderNoForAdmin(orderNo);
        return Result.success(order);
    }
}