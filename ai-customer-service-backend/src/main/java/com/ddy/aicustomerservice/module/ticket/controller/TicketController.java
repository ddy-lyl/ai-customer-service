package com.ddy.aicustomerservice.module.ticket.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:31
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.ticket.dto.TicketCancelRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketCloseRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketCreateRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketPageQuery;
import com.ddy.aicustomerservice.module.ticket.service.TicketService;
import com.ddy.aicustomerservice.module.ticket.vo.TicketVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 普通用户工单控制器
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 创建工单
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Result<TicketVO> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        TicketVO ticket = ticketService.createTicket(request);
        return Result.success(ticket);
    }

    /**
     * 当前用户分页查询自己的工单
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'ADMIN')")
    public Result<PageResult<TicketVO>> pageMyTickets(TicketPageQuery query) {
        PageResult<TicketVO> pageResult = ticketService.pageMyTickets(query);
        return Result.success(pageResult);
    }

    /**
     * 查看工单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'ADMIN')")
    public Result<TicketVO> getTicketDetail(@PathVariable Long id) {
        TicketVO ticket = ticketService.getTicketDetail(id);
        return Result.success(ticket);
    }

    /**
     * 关闭工单
     */
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'ADMIN')")
    public Result<Void> closeTicket(@PathVariable Long id,
                                    @RequestBody TicketCloseRequest request) {
        ticketService.closeTicket(id, request);
        return Result.success();
    }

    /**
     * 取消工单（仅待处理状态可取消）
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> cancelTicket(@PathVariable Long id,
                                     @RequestBody(required = false) TicketCancelRequest request) {
        ticketService.cancelTicket(id, request != null ? request : new TicketCancelRequest());
        return Result.success();
    }
}