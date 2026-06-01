package com.ddy.aicustomerservice.module.order.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/17 13:49
 **/

import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 客户订单实体类
 *
 * 对应数据库表：customer_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_order")
public class CustomerOrder extends BaseEntity {

    /**
     * 订单号
     *
     * 用户通常提供订单号，而不是数据库ID。
     */
    private String orderNo;

    /**
     * 下单用户ID
     *
     * 用于判断订单属于哪个用户。
     */
    private Long userId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品SKU
     */
    private String productSku;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单状态
     *
     * WAIT_PAY、WAIT_SHIPPING、SHIPPED、RECEIVED、REFUNDING、REFUNDED、CLOSED
     */
    private String status;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    private LocalDateTime shippingTime;

    /**
     * 签收时间
     */
    private LocalDateTime receivedTime;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人手机号
     */
    private String receiverPhone;

    /**
     * 收货地址
     */
    private String receiverAddress;

    /**
     * 订单备注
     */
    private String remark;
}