package com.ddy.aicustomerservice.module.ai.tool.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddy.aicustomerservice.common.constant.AiToolConstants;
import com.ddy.aicustomerservice.module.ai.tool.entity.AiToolCallLog;
import com.ddy.aicustomerservice.module.ai.tool.mapper.AiToolCallLogMapper;
import com.ddy.aicustomerservice.module.order.service.OrderService;
import com.ddy.aicustomerservice.module.order.vo.OrderVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 创建工单时的订单号解析：工具入参、问题描述、本会话查单记录、唯一订单兜底。
 */
@Component
@RequiredArgsConstructor
public class AiTicketOrderResolver {

    private static final Pattern ORDER_NO_PATTERN =
            Pattern.compile("ORD\\d{10,}", Pattern.CASE_INSENSITIVE);

    private final OrderService orderService;

    private final AiToolCallLogMapper aiToolCallLogMapper;

    private final ObjectMapper objectMapper;

    /**
     * 解析可用于建单的订单号；无法确定时返回 null。
     */
    public String resolveOrderNo(String providedOrderNo,
                                 Long sessionId,
                                 String title,
                                 String description) {
        if (StringUtils.hasText(providedOrderNo)) {
            return providedOrderNo.trim();
        }

        String fromText = extractOrderNoFromText(title, description);
        if (StringUtils.hasText(fromText)) {
            return fromText;
        }

        String fromSession = resolveFromSessionQueryOrder(sessionId);
        if (StringUtils.hasText(fromSession)) {
            return fromSession;
        }

        List<OrderVO> orders = orderService.listMyOrders();
        if (orders.size() == 1) {
            return orders.get(0).getOrderNo();
        }

        return null;
    }

    public List<Map<String, Object>> summarizeOrders(List<OrderVO> orders) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderVO order : orders) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orderNo", order.getOrderNo());
            item.put("productName", order.getProductName());
            item.put("status", order.getStatus());
            item.put("statusName", order.getStatusName());
            item.put("amount", order.getAmount());
            list.add(item);
        }
        return list;
    }

    private String extractOrderNoFromText(String... texts) {
        for (String text : texts) {
            if (!StringUtils.hasText(text)) {
                continue;
            }
            Matcher matcher = ORDER_NO_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group().toUpperCase();
            }
        }
        return null;
    }

    private String resolveFromSessionQueryOrder(Long sessionId) {
        if (sessionId == null) {
            return null;
        }

        AiToolCallLog log = aiToolCallLogMapper.selectOne(
                new LambdaQueryWrapper<AiToolCallLog>()
                        .eq(AiToolCallLog::getSessionId, sessionId)
                        .eq(AiToolCallLog::getToolName, AiToolConstants.TOOL_QUERY_ORDER)
                        .eq(AiToolCallLog::getStatus, AiToolConstants.STATUS_SUCCESS)
                        .orderByDesc(AiToolCallLog::getCreateTime)
                        .last("LIMIT 1")
        );

        if (log == null || !StringUtils.hasText(log.getRequestJson())) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(log.getRequestJson());
            JsonNode orderNoNode = root.get("orderNo");
            if (orderNoNode != null && orderNoNode.isTextual()) {
                return orderNoNode.asText().trim();
            }
        } catch (Exception ignored) {
            // 解析失败则走后续兜底
        }
        return null;
    }
}
