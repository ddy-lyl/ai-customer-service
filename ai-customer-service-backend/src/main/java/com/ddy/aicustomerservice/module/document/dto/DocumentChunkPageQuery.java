package com.ddy.aicustomerservice.module.document.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/19 9:41
 **/
import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档切片分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentChunkPageQuery extends PageQuery {

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 切片内容关键词
     */
    private String contentKeyword;

    /**
     * 是否已向量化：
     * 0 未向量化
     * 1 已向量化
     */
    private Integer vectorized;
}
