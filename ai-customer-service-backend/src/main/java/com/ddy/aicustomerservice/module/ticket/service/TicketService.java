package com.ddy.aicustomerservice.module.ticket.service;

import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.ticket.dto.*;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowTimelineVO;
import com.ddy.aicustomerservice.module.ticket.vo.TicketVO;

import java.util.List;

/**
 * 工单业务接口
 */
public interface TicketService {

    /**
     * 用户主动创建工单（source = USER）
     */
    TicketVO createTicket(TicketCreateRequest request);

    /**
     * 客服代客建单（source = STAFF，工单归属目标客户）
     */
    TicketVO createTicketOnBehalf(TicketCreateRequest request);

    /**
     * 用户取消待处理工单（PENDING → CANCELLED）
     */
    void cancelTicket(Long id, TicketCancelRequest request);

    /**
     * AI 工具调用创建工单（source = AI）
     *
     * 用于 AI 在多轮对话中识别到"用户希望转人工/申请售后"时自动创建。
     * 与 {@link #createTicket(TicketCreateRequest)} 的区别：
     * 1. source 固定为 AI
     * 2. 会写入 sessionId，便于客服回溯产生工单的对话
     * 3. operator 在流转记录中显示为"AI智能客服"
     *
     * @param request   工单内容
     * @param sessionId 触发工单创建的 AI 会话 ID，可为 null
     */
    TicketVO createTicketFromAi(TicketCreateRequest request, Long sessionId);

    /**
     * 在线客服转人工创建咨询工单（可不关联订单）
     */
    TicketVO createHandoffTicket(Long sessionId, Long userId, String reason, String description);

    /**
     * 当前用户分页查询自己的工单
     */
    PageResult<TicketVO> pageMyTickets(TicketPageQuery query);

    /**
     * 查看工单详情
     */
    TicketVO getTicketDetail(Long id);

    /**
     * 查询工单流转时间线
     */
    List<TicketFlowTimelineVO> listTicketTimeline(Long ticketId);

    /**
     * 客服分页查询工单（支持待认领池 / 我的工单）
     */
    PageResult<TicketVO> pageStaffTickets(TicketPageQuery query);

    /**
     * 客服认领待处理工单（PENDING 且未分配 → 绑定当前客服并进入 PROCESSING）
     */
    void claimTicket(Long id);

    /**
     * 管理员分页查询所有工单
     */
    PageResult<TicketVO> pageTicketsForAdmin(TicketPageQuery query);

    /**
     * 管理员分配工单
     */
    void assignTicket(Long id, TicketAssignRequest request);

    /**
     * 客服或管理员处理工单
     */
    void processTicket(Long id, TicketProcessRequest request);

    /**
     * 关闭工单
     */
    void closeTicket(Long id, TicketCloseRequest request);
}
