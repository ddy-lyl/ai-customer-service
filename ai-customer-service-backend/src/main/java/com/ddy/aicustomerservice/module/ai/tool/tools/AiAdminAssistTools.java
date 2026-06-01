package com.ddy.aicustomerservice.module.ai.tool.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddy.aicustomerservice.common.constant.AiToolConstants;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.enums.ChatMessageRoleEnum;
import com.ddy.aicustomerservice.module.ai.tool.entity.AiToolCallLog;
import com.ddy.aicustomerservice.module.ai.tool.mapper.AiToolCallLogMapper;
import com.ddy.aicustomerservice.module.ai.tool.support.AiToolResult;
import com.ddy.aicustomerservice.module.chat.entity.ChatMessage;
import com.ddy.aicustomerservice.module.chat.mapper.ChatMessageMapper;
import com.ddy.aicustomerservice.module.order.entity.CustomerOrder;
import com.ddy.aicustomerservice.module.order.mapper.CustomerOrderMapper;
import com.ddy.aicustomerservice.module.ticket.entity.Ticket;
import com.ddy.aicustomerservice.module.ticket.mapper.TicketMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员运营分析 AI 工具。
 */
@Component
@RequiredArgsConstructor
public class AiAdminAssistTools {

    private final CustomerOrderMapper customerOrderMapper;

    private final TicketMapper ticketMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final AiToolCallLogMapper aiToolCallLogMapper;

    private final ObjectMapper objectMapper;

    @Tool(
            name = AiToolConstants.TOOL_ADMIN_PLATFORM_OVERVIEW,
            description = """
                    查询平台运营概览：订单总量、工单状态分布、工单类型分布、近期 AI 工具失败次数。
                    用于管理员分析业务占比与客服负载，不要编造数据。
                    """
    )
    public String platformOverview() {
        LoginUserContext.getRequired();

        long orderTotal = customerOrderMapper.selectCount(null);

        List<Ticket> tickets = ticketMapper.selectList(null);
        Map<String, Long> statusCount = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getStatus, Collectors.counting()));
        Map<String, Long> typeCount = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getType, Collectors.counting()));

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        long failedTools = aiToolCallLogMapper.selectCount(
                new LambdaQueryWrapper<AiToolCallLog>()
                        .eq(AiToolCallLog::getStatus, AiToolConstants.STATUS_FAILED)
                        .ge(AiToolCallLog::getCreateTime, since)
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderTotal", orderTotal);
        data.put("ticketTotal", tickets.size());
        data.put("ticketByStatus", statusCount);
        data.put("ticketByType", typeCount);
        data.put("failedToolCallsLast7Days", failedTools);

        return toJson(AiToolResult.success("平台概览查询成功", data));
    }

    @Tool(
            name = AiToolConstants.TOOL_ADMIN_HOT_QUESTIONS,
            description = """
                    分析近期用户咨询热点：汇总最近消费者提问文本（用于 FAQ 优化、知识库补全）。
                    limit 为返回条数上限，默认 10。
                    """
    )
    public String hotQuestions(
            @ToolParam(description = "返回热点问题条数，默认 10，最大 20")
            Integer limit
    ) {
        LoginUserContext.getRequired();

        int safeLimit = limit == null ? 10 : Math.min(Math.max(limit, 1), 20);
        LocalDateTime since = LocalDateTime.now().minusDays(14);

        List<ChatMessage> recent = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRole, ChatMessageRoleEnum.USER.getCode())
                        .ge(ChatMessage::getCreateTime, since)
                        .orderByDesc(ChatMessage::getCreateTime)
                        .last("LIMIT 80")
        );

        Map<String, Long> freq = new LinkedHashMap<>();
        for (ChatMessage msg : recent) {
            if (msg.getContent() == null) {
                continue;
            }
            String key = msg.getContent().trim();
            if (key.length() > 80) {
                key = key.substring(0, 80) + "…";
            }
            freq.merge(key, 1L, Long::sum);
        }

        List<Map<String, Object>> hotspots = freq.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(safeLimit)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("question", e.getKey());
                    row.put("count", e.getValue());
                    return row;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("windowDays", 14);
        data.put("hotQuestions", hotspots);

        return toJson(AiToolResult.success("热点问题分析完成", data));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
