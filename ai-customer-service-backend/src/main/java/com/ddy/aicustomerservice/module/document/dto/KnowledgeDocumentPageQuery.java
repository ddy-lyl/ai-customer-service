package com.ddy.aicustomerservice.module.document.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/18 22:19
 **/
import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocumentPageQuery extends PageQuery {

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 原始文件名，支持模糊查询
     */
    private String originalFilename;

    /**
     * 文件类型：
     * pdf、docx、txt、md
     */
    private String fileType;

    /**
     * 文档处理状态：
     * UPLOADED、PARSING、PARSED、CHUNKED、VECTORIZED、FAILED
     */
    private String status;

    /**
     * 上传人ID
     */
    private Long uploadedBy;
}
