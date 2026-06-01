package com.ddy.aicustomerservice.module.ai.tool.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddy.aicustomerservice.common.constant.AiToolConstants;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.enums.OrderStatusEnum;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.common.enums.TicketStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.module.ai.tool.context.AiToolCallContext;
import com.ddy.aicustomerservice.module.ai.tool.context.AiToolCallContextInfo;
import com.ddy.aicustomerservice.module.ai.tool.entity.AiToolCallLog;
import com.ddy.aicustomerservice.module.ai.tool.mapper.AiToolCallLogMapper;
import com.ddy.aicustomerservice.module.ai.tool.support.AiTicketOrderResolver;
import com.ddy.aicustomerservice.module.ai.tool.support.AiToolResult;
import com.ddy.aicustomerservice.module.order.service.OrderService;
import com.ddy.aicustomerservice.module.order.vo.OrderVO;
import com.ddy.aicustomerservice.module.ticket.dto.TicketCreateRequest;
import com.ddy.aicustomerservice.module.ticket.entity.Ticket;
import com.ddy.aicustomerservice.module.ticket.mapper.TicketMapper;
import com.ddy.aicustomerservice.module.ticket.service.TicketService;
import com.ddy.aicustomerservice.module.ticket.vo.TicketVO;
import com.ddy.aicustomerservice.module.chat.support.RoleChatResolver;
import com.ddy.aicustomerservice.module.user.entity.SysUser;
import com.ddy.aicustomerservice.module.user.mapper.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AI 业务工具
 *
 * 这里的方法会暴露给大模型作为 Function Calling 工具，
 * 必须严格控制：
 * 1. 任何工具内部都要做权限校验（不能信任 LLM 传过来的参数）；
 * 2. 工具调用要落库（ai_tool_call_log），便于审计；
 * 3. 工具内部尽量调用 service 层，不要直接写数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiBusinessTools {

    private final TicketMapper ticketMapper;

    private final SysUserMapper sysUserMapper;

    private final AiToolCallLogMapper aiToolCallLogMapper;

    private final ObjectMapper objectMapper;

    private final OrderService orderService;

    private final TicketService ticketService;

    private final AiTicketOrderResolver aiTicketOrderResolver;

    @Tool(
            name = AiToolConstants.TOOL_QUERY_ORDER,
            description = """
                    根据订单号查询订单状态和基本信息。
                    普通用户只能查本人订单；客服/管理员可按订单号查全平台订单（处理工单时使用）。
                    当询问订单状态、物流、售后关联订单时使用。无订单号时不要调用。
                    """
    )
    public String queryOrder(
            @ToolParam(description = "订单号，例如 202605180001")
            String orderNo
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("orderNo", orderNo);

        return executeTool(
                AiToolConstants.TOOL_QUERY_ORDER,
                request,
                () -> doQueryOrder(orderNo)
        );
    }

    @Tool(
            name = AiToolConstants.TOOL_LIST_MY_ORDERS,
            description = """
                    列出当前登录用户本人的订单列表（订单号、商品、状态）。
                    当用户要申请售后/创建工单但未说明订单号时，必须先调用本工具，让用户选择要处理的订单。
                    用户已明确给出订单号时，请直接调用 QUERY_ORDER 或 CREATE_TICKET，无需重复列出。
                    """
    )
    public String listMyOrders() {
        return executeTool(
                AiToolConstants.TOOL_LIST_MY_ORDERS,
                Map.of(),
                this::doListMyOrders
        );
    }

    @Tool(
            name = AiToolConstants.TOOL_CREATE_TICKET,
            description = """
                    创建售后工单。必须关联用户本人的有效订单号 orderNo，禁止传空。
                    当用户明确要求申请售后、申请退货退款、商品质量问题处理、转人工客服、创建工单时使用。
                    用户未提供订单号时：先调用 LIST_MY_ORDERS 让用户选单，或根据本会话刚查询过的订单号填写 orderNo。
                    用户只是咨询售后政策时，不要调用该工具。
                    """
    )
    public String createTicket(
            @ToolParam(description = "关联订单号，必填。例如 ORD202605010001；可从用户话术、本会话 QUERY_ORDER 结果中获取")
            String orderNo,

            @ToolParam(description = "工单标题，简短概括用户问题")
            String title,

            @ToolParam(description = "工单类型。可选：REFUND、RETURN_GOODS、EXCHANGE、LOGISTICS、INVOICE、PRODUCT_QUALITY、ACCOUNT、OTHER")
            String type,

            @ToolParam(description = "用户问题详细描述")
            String description,

            @ToolParam(description = "优先级。可选：LOW、MEDIUM、HIGH、URGENT。没有明确优先级时传 MEDIUM")
            String priority
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("orderNo", orderNo);
        request.put("title", title);
        request.put("type", type);
        request.put("description", description);
        request.put("priority", priority);

        return executeTool(
                AiToolConstants.TOOL_CREATE_TICKET,
                request,
                () -> doCreateTicket(orderNo, title, type, description, priority)
        );
    }

    @Tool(
            name = AiToolConstants.TOOL_QUERY_TICKET_STATUS,
            description = """
                    根据工单编号查询工单处理状态。
                    当用户询问工单进度、售后处理到哪了、工单是否解决时使用。
                    如果用户没有提供工单编号，不要调用该工具，应要求用户补充工单编号。
                    """
    )
    public String queryTicketStatus(
            @ToolParam(description = "工单编号，例如 TK202605190001")
            String ticketNo
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("ticketNo", ticketNo);

        return executeTool(
                AiToolConstants.TOOL_QUERY_TICKET_STATUS,
                request,
                () -> doQueryTicketStatus(ticketNo)
        );
    }

    /**
     * 执行订单查询
     *
     * 走 OrderService.queryOrderForAi：USER 仅本人订单，STAFF/ADMIN 可查全平台。
     */
    private AiToolResult doQueryOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return AiToolResult.fail("订单号不能为空");
        }

        try {
            OrderVO order = orderService.queryOrderForAi(orderNo.trim());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderId", order.getId());
            data.put("orderNo", order.getOrderNo());
            data.put("status", order.getStatus());
            data.put("statusName", order.getStatusName());
            data.put("productName", order.getProductName());
            data.put("amount", order.getAmount());
            data.put("payTime", order.getPayTime());
            data.put("shippingTime", order.getShippingTime());
            data.put("receivedTime", order.getReceivedTime());

            return AiToolResult.success("订单查询成功", data);
        } catch (BusinessException e) {
            return AiToolResult.fail(e.getMessage());
        }
    }

    /**
     * 列出当前用户订单，供建工单前选单。
     */
    private AiToolResult doListMyOrders() {
        LoginUserInfo operator = LoginUserContext.getRequired();
        if (!RoleChatResolver.isEndUser(operator)) {
            return AiToolResult.fail("列出订单仅面向消费者账号");
        }

        List<OrderVO> orders = orderService.listMyOrders();
        if (orders.isEmpty()) {
            return AiToolResult.fail("您暂无订单，无法申请售后");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orders", aiTicketOrderResolver.summarizeOrders(orders));
        data.put("count", orders.size());

        return AiToolResult.success("已查询到您的订单列表，请让用户选择要售后的订单号", data);
    }

    /**
     * 执行创建售后工单
     *
     * 与用户主动建单一样走 TicketService 来保证业务一致性，
     * 但 source 固定为 AI，并把当前会话 ID 写入 ticket.sessionId
     * 以建立"AI 会话 -> 工单"的溯源链路。
     */
    private AiToolResult doCreateTicket(String orderNo,
                                        String title,
                                        String type,
                                        String description,
                                        String priority) {
        if (!StringUtils.hasText(title)) {
            return AiToolResult.fail("工单标题不能为空");
        }
        if (!StringUtils.hasText(description)) {
            return AiToolResult.fail("工单描述不能为空");
        }

        LoginUserInfo operator = LoginUserContext.getRequired();
        if (!RoleChatResolver.isEndUser(operator)) {
            return AiToolResult.fail("创建售后工单仅面向消费者账号，客服/管理员请在工单工作台处理");
        }

        AiToolCallContextInfo contextInfo = AiToolCallContext.get();
        Long sessionId = contextInfo == null ? null : contextInfo.getSessionId();

        String resolvedOrderNo = aiTicketOrderResolver.resolveOrderNo(
                orderNo,
                sessionId,
                title,
                description
        );

        if (!StringUtils.hasText(resolvedOrderNo)) {
            List<OrderVO> orders = orderService.listMyOrders();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orders", aiTicketOrderResolver.summarizeOrders(orders));
            data.put("hint", "请先调用 LIST_MY_ORDERS 向用户展示订单并确认 orderNo，再调用 CREATE_TICKET");
            return AiToolResult.fail("创建售后工单必须关联订单，请让用户选择订单号", data);
        }

        TicketCreateRequest request = new TicketCreateRequest();
        request.setOrderNo(resolvedOrderNo);
        request.setTitle(title);
        request.setType(type);
        request.setDescription(description);
        request.setPriority(priority);

        try {
            TicketVO ticket = ticketService.createTicketFromAi(request, sessionId);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderNo", ticket.getOrderNo());
            data.put("ticketId", ticket.getId());
            data.put("ticketNo", ticket.getTicketNo());
            data.put("title", ticket.getTitle());
            data.put("type", ticket.getType());
            data.put("typeName", ticket.getTypeName());
            data.put("status", ticket.getStatus());
            data.put("statusName", ticket.getStatusName());
            data.put("priority", ticket.getPriority());
            data.put("priorityName", ticket.getPriorityName());
            data.put("source", ticket.getSource());
            data.put("sourceName", ticket.getSourceName());

            return AiToolResult.success("售后工单创建成功", data);
        } catch (BusinessException e) {
            return AiToolResult.fail(e.getMessage());
        }
    }

    /**
     * 执行工单状态查询
     *
     * AI 工具默认只允许查询当前登录用户名下的工单。
     * 管理员需要查别人的工单请走 /api/admin/tickets 接口。
     */
    private AiToolResult doQueryTicketStatus(String ticketNo) {
        if (!StringUtils.hasText(ticketNo)) {
            return AiToolResult.fail("工单编号不能为空");
        }

        LoginUserInfo currentUser = LoginUserContext.getRequired();

        Ticket ticket = ticketMapper.selectOne(
                new LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getTicketNo, ticketNo.trim())
                        .last("LIMIT 1")
        );

        if (ticket == null) {
            return AiToolResult.fail("没有查询到该工单");
        }

        if (!canViewTicketViaAi(currentUser, ticket)) {
            return AiToolResult.fail("没有权限查询该工单");
        }

        String staffName = "";
        if (ticket.getStaffId() != null) {
            SysUser staff = sysUserMapper.selectById(ticket.getStaffId());
            if (staff != null) {
                staffName = StringUtils.hasText(staff.getNickname())
                        ? staff.getNickname()
                        : staff.getUsername();
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ticketId", ticket.getId());
        data.put("ticketNo", ticket.getTicketNo());
        data.put("title", ticket.getTitle());
        data.put("type", ticket.getType());
        data.put("status", ticket.getStatus());
        data.put("statusName", convertTicketStatusName(ticket.getStatus()));
        data.put("priority", ticket.getPriority());
        data.put("staffId", ticket.getStaffId());
        data.put("staffName", staffName);
        data.put("processResult", ticket.getProcessResult() == null ? "" : ticket.getProcessResult());

        return AiToolResult.success("工单状态查询成功", data);
    }

    /**
     * 统一执行工具并记录 ai_tool_call_log
     */
    private String executeTool(String toolName,
                               Object request,
                               Supplier<AiToolResult> supplier) {
        long start = System.currentTimeMillis();

        AiToolCallContextInfo contextInfo = AiToolCallContext.get();

        AiToolCallLog callLog = new AiToolCallLog();
        callLog.setSessionId(contextInfo == null ? null : contextInfo.getSessionId());
        callLog.setUserId(contextInfo == null ? LoginUserContext.getUserId() : contextInfo.getUserId());
        callLog.setUserMessageId(contextInfo == null ? null : contextInfo.getUserMessageId());
        callLog.setToolName(toolName);
        callLog.setRequestJson(toJson(request));

        try {
            AiToolResult result = supplier.get();

            long costMillis = System.currentTimeMillis() - start;

            callLog.setResponseJson(toJson(result));
            callLog.setStatus(Boolean.TRUE.equals(result.getSuccess())
                    ? AiToolConstants.STATUS_SUCCESS
                    : AiToolConstants.STATUS_FAILED);
            callLog.setErrorMessage(Boolean.TRUE.equals(result.getSuccess())
                    ? null
                    : result.getMessage());
            callLog.setCostMillis(costMillis);

            aiToolCallLogMapper.insert(callLog);

            return toJson(result);

        } catch (Exception e) {
            log.error("[AI 工具调用异常] tool={}", toolName, e);

            long costMillis = System.currentTimeMillis() - start;

            AiToolResult failResult = AiToolResult.fail("工具调用异常：" + e.getMessage());

            callLog.setResponseJson(toJson(failResult));
            callLog.setStatus(AiToolConstants.STATUS_FAILED);
            callLog.setErrorMessage(e.getMessage());
            callLog.setCostMillis(costMillis);

            aiToolCallLogMapper.insert(callLog);

            return toJson(failResult);
        }
    }

    /**
     * AI 工具查工单的权限：
     * ADMIN：任意；
     * STAFF：自己负责的或待认领池中的工单；
     * USER：只能查自己创建的。
     */
    private boolean canViewTicketViaAi(LoginUserInfo currentUser, Ticket ticket) {
        if (currentUser == null || ticket == null) {
            return false;
        }
        if (currentUser.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            return true;
        }
        if (currentUser.hasRole(RoleCodeEnum.STAFF.getCode())) {
            if (currentUser.getUserId().equals(ticket.getStaffId())) {
                return true;
            }
            return ticket.getStaffId() == null
                    && TicketStatusEnum.PENDING.getCode().equals(ticket.getStatus());
        }
        return currentUser.getUserId().equals(ticket.getUserId());
    }

    private String convertTicketStatusName(String status) {
        if (status == null) {
            return null;
        }
        for (TicketStatusEnum item : TicketStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                return item.getName();
            }
        }
        return "未知状态";
    }

    /**
     * 仅在 doQueryOrder 失败兜底场景使用。
     * 主流程依赖 OrderVO.statusName。
     */
    @SuppressWarnings("unused")
    private String convertOrderStatusName(String status) {
        if (status == null) {
            return null;
        }
        for (OrderStatusEnum item : OrderStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                return item.getName();
            }
        }
        return status;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
