package com.ddy.aicustomerservice.module.ticket.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:32
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.ticket.dto.TicketAssignRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketPageQuery;
import com.ddy.aicustomerservice.module.ticket.service.TicketService;
import com.ddy.aicustomerservice.module.ticket.vo.TicketVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员工单控制器
 */
@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTicketController {

    private final TicketService ticketService;

    /**
     * 管理员分页查询全部工单
     */
    @GetMapping("/page")
    public Result<PageResult<TicketVO>> pageTickets(TicketPageQuery query) {
        PageResult<TicketVO> pageResult = ticketService.pageTicketsForAdmin(query);
        return Result.success(pageResult);
    }

    /**
     * 管理员将未分配工单分配给客服（管理员不处理工单，不写入 staff_id 为管理员）
     */
    @PutMapping("/{id}/assign")
    public Result<Void> assignTicket(@PathVariable Long id,
                                     @Valid @RequestBody TicketAssignRequest request) {
        ticketService.assignTicket(id, request);
        return Result.success();
    }
}