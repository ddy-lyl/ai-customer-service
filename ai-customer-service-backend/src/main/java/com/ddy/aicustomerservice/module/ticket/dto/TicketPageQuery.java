package com.ddy.aicustomerservice.module.ticket.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:25
 **/
import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TicketPageQuery extends PageQuery {

    /**
     * 工单编号
     */
    private String ticketNo;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 客服ID
     */
    private Long staffId;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 工单状态
     */
    private String status;

    /**
     * 工单类型
     */
    private String type;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 工单来源：USER / AI
     */
    private String source;

    /**
     * 关键词：模糊匹配工单号或标题
     */
    private String keyword;

    /**
     * 客服列表范围（仅客服端使用，必填其一）：
     * POOL — 待认领（PENDING 且未分配客服）
     * MINE — 我负责/已认领的工单
     */
    private String scope;
}
