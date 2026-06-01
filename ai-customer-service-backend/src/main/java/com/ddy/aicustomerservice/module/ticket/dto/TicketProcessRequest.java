package com.ddy.aicustomerservice.module.ticket.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:26
 **/
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 处理工单请求对象
 */
@Data
public class TicketProcessRequest {

    /**
     * 处理结果
     */
    @NotBlank(message = "处理结果不能为空")
    private String processResult;

    /**
     * 处理备注
     */
    private String remark;

    /**
     * 是否标记为已解决
     *
     * true：处理后改成 RESOLVED
     * false：仍然保持 PROCESSING
     */
    private Boolean resolved;
}