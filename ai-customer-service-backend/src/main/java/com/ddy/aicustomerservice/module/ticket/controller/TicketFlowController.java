package com.ddy.aicustomerservice.module.ticket.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/17 16:37
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.ticket.dto.TicketFlowPageQuery;
import com.ddy.aicustomerservice.module.ticket.service.TicketFlowService;
import com.ddy.aicustomerservice.module.ticket.service.TicketService;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowTimelineVO;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单流转记录控制器
 */
@RestController
@RequiredArgsConstructor
public class TicketFlowController {

    private final TicketService ticketService;

    private final TicketFlowService ticketFlowService;

    /**
     * 查询某个工单的流转时间线
     *
     * USER：只能看自己的工单
     * STAFF：只能看分配给自己的工单
     * ADMIN：可以看全部
     */
    @GetMapping("/api/tickets/{ticketId}/flows")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'ADMIN')")
    public Result<List<TicketFlowTimelineVO>> listTicketTimeline(@PathVariable Long ticketId) {
        List<TicketFlowTimelineVO> timeline = ticketService.listTicketTimeline(ticketId);
        return Result.success(timeline);
    }

    /**
     * 管理员分页查询所有工单流转记录
     */
    @GetMapping("/api/admin/ticket-flows/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<TicketFlowVO>> pageTicketFlows(TicketFlowPageQuery query) {
        PageResult<TicketFlowVO> pageResult = ticketFlowService.pageFlowsForAdmin(query);
        return Result.success(pageResult);
    }
}