package com.ddy.aicustomerservice.module.ticket.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 取消工单请求
 */
@Data
public class TicketCancelRequest {

    @Size(max = 500, message = "取消原因不能超过500个字符")
    private String cancelReason;
}
