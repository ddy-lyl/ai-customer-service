package com.ddy.aicustomerservice.module.ticket.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:32
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.ticket.dto.TicketCreateRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketPageQuery;
import com.ddy.aicustomerservice.module.ticket.dto.TicketProcessRequest;
import com.ddy.aicustomerservice.module.ticket.service.TicketService;
import com.ddy.aicustomerservice.module.ticket.vo.TicketVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 客服工单控制器
 */
@RestController
@RequestMapping("/api/staff/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffTicketController {

    private final TicketService ticketService;

    /**
     * 代客建单（必须传 onBehalfUserId）
     */
    @PostMapping
    public Result<TicketVO> createOnBehalf(@Valid @RequestBody TicketCreateRequest request) {
        return Result.success(ticketService.createTicketOnBehalf(request));
    }

    /**
     * 客服分页查询工单（scope 必填：POOL=待认领，MINE=我负责的）
     */
    @GetMapping("/page")
    public Result<PageResult<TicketVO>> pageStaffTickets(TicketPageQuery query) {
        PageResult<TicketVO> pageResult = ticketService.pageStaffTickets(query);
        return Result.success(pageResult);
    }

    /**
     * 客服认领待处理工单
     */
    @PutMapping("/{id}/claim")
    public Result<Void> claimTicket(@PathVariable Long id) {
        ticketService.claimTicket(id);
        return Result.success();
    }

    /**
     * 客服处理工单
     */
    @PutMapping("/{id}/process")
    public Result<Void> processTicket(@PathVariable Long id,
                                      @Valid @RequestBody TicketProcessRequest request) {
        ticketService.processTicket(id, request);
        return Result.success();
    }
}