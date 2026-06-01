package com.ddy.aicustomerservice.module.order.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/17 13:53
 **/
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.enums.OrderStatusEnum;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.order.dto.OrderPageQuery;
import com.ddy.aicustomerservice.module.order.entity.CustomerOrder;
import com.ddy.aicustomerservice.module.order.mapper.CustomerOrderMapper;
import com.ddy.aicustomerservice.module.order.service.OrderService;
import com.ddy.aicustomerservice.module.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 订单业务实现类
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CustomerOrderMapper customerOrderMapper;

    /**
     * 查询当前登录用户的订单列表
     */
    @Override
    public List<OrderVO> listMyOrders() {
        Long currentUserId = LoginUserContext.getUserId();

        List<CustomerOrder> orders = customerOrderMapper.selectList(
                new LambdaQueryWrapper<CustomerOrder>()
                        .eq(CustomerOrder::getUserId, currentUserId)
                        .orderByDesc(CustomerOrder::getCreateTime)
        );

        return orders.stream()
                .map(this::convertToOrderVO)
                .toList();
    }

    /**
     * 当前用户根据订单号查询自己的订单
     */
    @Override
    public OrderVO getMyOrderByOrderNo(String orderNo) {
        Long currentUserId = LoginUserContext.getUserId();

        CustomerOrder order = customerOrderMapper.selectOne(
                new LambdaQueryWrapper<CustomerOrder>()
                        .eq(CustomerOrder::getOrderNo, orderNo)
                        .eq(CustomerOrder::getUserId, currentUserId)
                        .last("LIMIT 1")
        );

        if (order == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "订单不存在或无权查看该订单"
            );
        }

        return convertToOrderVO(order);
    }

    /**
     * 管理员分页查询订单
     */
    @Override
    public PageResult<OrderVO> pageOrdersForAdmin(OrderPageQuery query) {
        Page<CustomerOrder> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<CustomerOrder> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(query.getOrderNo()),
                CustomerOrder::getOrderNo,
                query.getOrderNo());

        wrapper.eq(query.getUserId() != null,
                CustomerOrder::getUserId,
                query.getUserId());

        wrapper.like(StringUtils.hasText(query.getProductName()),
                CustomerOrder::getProductName,
                query.getProductName());

        wrapper.eq(StringUtils.hasText(query.getStatus()),
                CustomerOrder::getStatus,
                query.getStatus());

        wrapper.orderByDesc(CustomerOrder::getCreateTime);

        Page<CustomerOrder> orderPage = customerOrderMapper.selectPage(page, wrapper);

        List<OrderVO> records = orderPage.getRecords()
                .stream()
                .map(this::convertToOrderVO)
                .toList();

        return PageResult.of(
                orderPage.getCurrent(),
                orderPage.getSize(),
                orderPage.getTotal(),
                orderPage.getPages(),
                records
        );
    }

    /**
     * 管理员根据订单号查询订单
     */
    @Override
    public OrderVO getOrderByOrderNoForAdmin(String orderNo) {
        return getOrderByOrderNoOrThrow(orderNo);
    }

    /**
     * 客服根据订单号查询订单
     *
     * 客服在工单详情页核对订单时使用。
     * 直接复用按订单号查询的逻辑，不做用户归属过滤。
     */
    @Override
    public OrderVO getOrderByOrderNoForStaff(String orderNo) {
        return getOrderByOrderNoOrThrow(orderNo);
    }

    private OrderVO getOrderByOrderNoOrThrow(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "订单号不能为空"
            );
        }
        CustomerOrder order = customerOrderMapper.selectOne(
                new LambdaQueryWrapper<CustomerOrder>()
                        .eq(CustomerOrder::getOrderNo, orderNo.trim())
                        .last("LIMIT 1")
        );
        if (order == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "订单不存在"
            );
        }
        return convertToOrderVO(order);
    }

    /**
     * AI 工具按角色查询订单：C 端仅本人；客服/管理员可查全平台。
     */
    @Override
    public OrderVO queryOrderForAi(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "订单号不能为空"
            );
        }

        LoginUserInfo currentUser = LoginUserContext.getRequired();
        if (currentUser.hasRole(RoleCodeEnum.ADMIN.getCode())
                || currentUser.hasRole(RoleCodeEnum.STAFF.getCode())) {
            return getOrderByOrderNoOrThrow(orderNo.trim());
        }
        return getMyOrderByOrderNo(orderNo);
    }

    /**
     * Entity 转 VO
     */
    private OrderVO convertToOrderVO(CustomerOrder order) {
        OrderVO vo = new OrderVO();

        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setProductName(order.getProductName());
        vo.setProductSku(order.getProductSku());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusName(convertOrderStatusName(order.getStatus()));
        vo.setPayTime(order.getPayTime());
        vo.setShippingTime(order.getShippingTime());
        vo.setReceivedTime(order.getReceivedTime());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        return vo;
    }

    /**
     * 订单状态编码转中文名称
     */
    private String convertOrderStatusName(String status) {
        if (!StringUtils.hasText(status)) {
            return "未知状态";
        }

        for (OrderStatusEnum statusEnum : OrderStatusEnum.values()) {
            if (statusEnum.getCode().equals(status)) {
                return statusEnum.getName();
            }
        }

        return "未知状态";
    }
}
