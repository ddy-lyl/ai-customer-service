package com.ddy.aicustomerservice.module.ticket.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:26
 **/
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分配工单请求对象
 */
@Data
public class TicketAssignRequest {

    /**
     * 被分配的客服ID
     */
    @NotNull(message = "客服ID不能为空")
    private Long staffId;

    /**
     * 分配备注
     */
    private String remark;
}
