package com.ddy.aicustomerservice.module.order.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/17 13:51
 **/
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应对象
 *
 * 返回给前端和 AI 工具调用结果使用。
 */
@Data
public class OrderVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
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
     * 订单状态编码
     *
     * 例如：WAIT_SHIPPING
     */
    private String status;

    /**
     * 订单状态名称
     *
     * 例如：待发货
     */
    private String statusName;

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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}