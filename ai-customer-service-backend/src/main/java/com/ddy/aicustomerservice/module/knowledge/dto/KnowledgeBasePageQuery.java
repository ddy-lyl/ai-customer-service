package com.ddy.aicustomerservice.module.knowledge.dto;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:11
 **/
import com.ddy.aicustomerservice.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分页查询请求对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBasePageQuery extends PageQuery {

    /**
     * 知识库名称，支持模糊查询
     */
    private String name;

    /**
     * 知识库状态：
     * ENABLED / DISABLED
     */
    private String status;
}
