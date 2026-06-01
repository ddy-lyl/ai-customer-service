package com.ddy.aicustomerservice.module.ticket.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:25
 **/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工单请求对象
 */
@Data
public class TicketCreateRequest {

    /**
     * 关联订单号
     *
     * 有些问题不关联订单，可以为空。
     */
    private String orderNo;

    /**
     * 工单标题
     */
    @NotBlank(message = "工单标题不能为空")
    @Size(max = 100, message = "工单标题不能超过100个字符")
    private String title;

    /**
     * 工单类型
     *
     * REFUND、RETURN_GOODS、EXCHANGE、LOGISTICS、INVOICE、PRODUCT_QUALITY、ACCOUNT、OTHER
     */
    @NotBlank(message = "工单类型不能为空")
    private String type;

    /**
     * 问题描述
     */
    @NotBlank(message = "问题描述不能为空")
    @Size(max = 2000, message = "问题描述不能超过2000个字符")
    private String description;

    /**
     * 优先级
     *
     * LOW、MEDIUM、HIGH、URGENT
     * 普通用户可以不传，后端默认 MEDIUM。
     */
    private String priority;

    /**
     * 代客建单：目标客户用户 ID。
     *
     * 仅客服/管理员通过 {@code POST /api/staff/tickets} 代建时必填；
     * 普通用户自建工单时不得传入。
     */
    private Long onBehalfUserId;
}