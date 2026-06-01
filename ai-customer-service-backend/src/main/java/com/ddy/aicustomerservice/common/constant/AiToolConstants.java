package com.ddy.aicustomerservice.common.constant;

/**
 * @author 罗亚兰
 * @date 2026/5/19 22:43
 **/
/**
 * AI 工具调用常量
 */
public class AiToolConstants {

    private AiToolConstants() {
    }

    /**
     * 工具名称：查询订单
     */
    public static final String TOOL_QUERY_ORDER = "QUERY_ORDER";

    /**
     * 工具名称：列出当前用户订单（建工单前选单）
     */
    public static final String TOOL_LIST_MY_ORDERS = "LIST_MY_ORDERS";

    /**
     * 工具名称：创建工单
     */
    public static final String TOOL_CREATE_TICKET = "CREATE_TICKET";

    /**
     * 工具名称：查询工单状态
     */
    public static final String TOOL_QUERY_TICKET_STATUS = "QUERY_TICKET_STATUS";

    /**
     * 工具名称：客服列出待处理/我的工单
     */
    public static final String TOOL_LIST_STAFF_TICKETS = "LIST_STAFF_TICKETS";

    /**
     * 工具名称：管理员平台运营概览
     */
    public static final String TOOL_ADMIN_PLATFORM_OVERVIEW = "ADMIN_PLATFORM_OVERVIEW";

    /**
     * 工具名称：管理员热点问题分析
     */
    public static final String TOOL_ADMIN_HOT_QUESTIONS = "ADMIN_HOT_QUESTIONS";

    /**
     * 工具调用成功
     */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * 工具调用失败
     */
    public static final String STATUS_FAILED = "FAILED";
}
