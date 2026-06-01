package com.ddy.aicustomerservice.module.ticket.service;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:28
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.ticket.dto.TicketFlowPageQuery;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowTimelineVO;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowVO;

import java.util.List;

/**
 * 工单流转记录业务接口
 */
public interface TicketFlowService {

    /**
     * 记录工单流转
     */
    void recordFlow(Long ticketId,
                    Long operatorId,
                    String operatorName,
                    String operatorRole,
                    String action,
                    String fromStatus,
                    String toStatus,
                    String remark);

    /**
     * 查询工单流转记录
     */
    List<TicketFlowVO> listByTicketId(Long ticketId);

    /**
     * 查询工单时间线
     */
    List<TicketFlowTimelineVO> listTimelineByTicketId(Long ticketId);

    /**
     * 管理员分页查询所有工单流转记录
     */
    PageResult<TicketFlowVO> pageFlowsForAdmin(TicketFlowPageQuery query);
}