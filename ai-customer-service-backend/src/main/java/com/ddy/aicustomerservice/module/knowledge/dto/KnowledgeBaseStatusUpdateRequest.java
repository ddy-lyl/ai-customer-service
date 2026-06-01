package com.ddy.aicustomerservice.module.knowledge.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:11
 **/
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改知识库状态请求对象
 */
@Data
public class KnowledgeBaseStatusUpdateRequest {

    /**
     * 状态：
     * ENABLED 启用
     * DISABLED 禁用
     */
    @NotBlank(message = "知识库状态不能为空")
    private String status;
}