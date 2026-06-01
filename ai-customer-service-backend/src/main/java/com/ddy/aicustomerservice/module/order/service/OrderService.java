package com.ddy.aicustomerservice.module.order.service;

/**
 * @author 罗亚兰
 * @date 2026/5/17 13:52
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.order.dto.OrderPageQuery;
import com.ddy.aicustomerservice.module.order.vo.OrderVO;

import java.util.List;

/**
 * 订单业务接口
 */
public interface OrderService {

    /**
     * 查询当前登录用户的订单列表
     *
     * @return 当前用户订单列表
     */
    List<OrderVO> listMyOrders();

    /**
     * 当前用户根据订单号查询自己的订单
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderVO getMyOrderByOrderNo(String orderNo);

    /**
     * 管理员分页查询订单
     *
     * @param query 查询条件
     * @return 分页订单列表
     */
    PageResult<OrderVO> pageOrdersForAdmin(OrderPageQuery query);

    /**
     * 管理员根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderVO getOrderByOrderNoForAdmin(String orderNo);

    /**
     * 客服根据订单号查询订单
     *
     * 用于客服在处理工单时核对用户订单信息。
     * 与管理员查询的差别在 Controller 层做权限拦截，
     * 这里在 Service 层提供同样的只读语义。
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderVO getOrderByOrderNoForStaff(String orderNo);

    /**
     * 给 AI 工具调用使用的订单查询（按角色授权）
     *
     * USER：仅本人订单；
     * STAFF / ADMIN：可按订单号查询全平台订单（处理工单/运营核对）。
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    OrderVO queryOrderForAi(String orderNo);
}