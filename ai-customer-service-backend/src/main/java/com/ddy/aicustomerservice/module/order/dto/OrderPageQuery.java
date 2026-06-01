package com.ddy.aicustomerservice.module.order.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 13:51
 **/
import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageQuery extends PageQuery {

    /**
     * 订单号，支持模糊查询
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品名称，支持模糊查询
     */
    private String productName;

    /**
     * 订单状态
     *
     * WAIT_PAY、WAIT_SHIPPING、SHIPPED、RECEIVED、REFUNDING、REFUNDED、CLOSED
     */
    private String status;
}
