package com.ddy.aicustomerservice.module.knowledge.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:10
 **/
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增知识库请求对象
 */
@Data
public class KnowledgeBaseCreateRequest {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称不能超过100个字符")
    private String name;

    /**
     * 知识库描述
     */
    @Size(max = 500, message = "知识库描述不能超过500个字符")
    private String description;
}
