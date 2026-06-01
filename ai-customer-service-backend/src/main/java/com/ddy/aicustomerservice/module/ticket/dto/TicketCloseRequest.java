package com.ddy.aicustomerservice.module.ticket.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:26
 **/
import lombok.Data;

/**
 * 关闭工单请求对象
 */
@Data
public class TicketCloseRequest {

    /**
     * 关闭原因
     */
    private String closeReason;
}
