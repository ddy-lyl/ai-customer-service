package com.ddy.aicustomerservice.module.document.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/18 22:19
 **/
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库文档状态修改请求对象
 */
@Data
public class KnowledgeDocumentStatusUpdateRequest {

    /**
     * 文档状态：
     * UPLOADED、PARSING、PARSED、CHUNKED、VECTORIZED、FAILED
     */
    @NotBlank(message = "文档状态不能为空")
    private String status;

    /**
     * 失败原因
     *
     * 当 status = FAILED 时，可以填写失败原因。
     */
    private String failReason;
}