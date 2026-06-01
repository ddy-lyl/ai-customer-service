package com.ddy.aicustomerservice.module.ai.tool.tools;

import com.ddy.aicustomerservice.common.constant.AiToolConstants;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.ai.tool.support.AiToolResult;
import com.ddy.aicustomerservice.module.ticket.dto.TicketPageQuery;
import com.ddy.aicustomerservice.module.ticket.service.TicketService;
import com.ddy.aicustomerservice.module.ticket.vo.TicketVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客服工作台 AI 辅助工具（列工单等）。
 */
@Component
@RequiredArgsConstructor
public class AiStaffAssistTools {

    private final TicketService ticketService;

    private final ObjectMapper objectMapper;

    @Tool(
            name = AiToolConstants.TOOL_LIST_STAFF_TICKETS,
            description = """
                    列出客服待处理或已分配的工单摘要，用于工作台总览、判断优先级、推荐处理方案。
                    scope: POOL=待认领池, MINE=我负责的处理中工单（二选一，不要混用）。
                    """
    )
    public String listStaffTickets(
            @ToolParam(description = "范围：POOL 或 MINE，默认 POOL")
            String scope
    ) {
        LoginUserContext.getRequired();

        String safeScope = StringUtils.hasText(scope) ? scope.trim().toUpperCase() : "POOL";
        if (!List.of("POOL", "MINE").contains(safeScope)) {
            safeScope = "POOL";
        }

        TicketPageQuery query = new TicketPageQuery();
        query.setPageNo(1L);
        query.setPageSize(8L);
        query.setScope(safeScope);

        try {
            PageResult<TicketVO> page = ticketService.pageStaffTickets(query);
            List<Map<String, Object>> items = new ArrayList<>();
            for (TicketVO t : page.getRecords()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ticketNo", t.getTicketNo());
                row.put("title", t.getTitle());
                row.put("status", t.getStatus());
                row.put("statusName", t.getStatusName());
                row.put("priority", t.getPriority());
                row.put("priorityName", t.getPriorityName());
                row.put("type", t.getType());
                row.put("typeName", t.getTypeName());
                row.put("orderNo", t.getOrderNo());
                row.put("userNickname", t.getUserNickname());
                items.add(row);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("scope", safeScope);
            data.put("total", page.getTotal());
            data.put("tickets", items);

            return toJson(AiToolResult.success("工单列表查询成功", data));
        } catch (Exception e) {
            return toJson(AiToolResult.fail(e.getMessage()));
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
