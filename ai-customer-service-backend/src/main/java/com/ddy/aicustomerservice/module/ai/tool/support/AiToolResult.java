package com.ddy.aicustomerservice.module.ai.tool.support;

/**
 * @author 罗亚兰
 * @date 2026/5/19 22:48
 **/
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 工具统一返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiToolResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 返回数据
     */
    private Object data;

    public static AiToolResult success(String message, Object data) {
        return new AiToolResult(true, message, data);
    }

    public static AiToolResult fail(String message) {
        return new AiToolResult(false, message, null);
    }

    public static AiToolResult fail(String message, Object data) {
        return new AiToolResult(false, message, data);
    }
}