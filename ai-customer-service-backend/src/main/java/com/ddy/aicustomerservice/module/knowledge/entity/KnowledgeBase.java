package com.ddy.aicustomerservice.module.knowledge.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:06
 **/
import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体类
 *
 * 对应数据库表：knowledge_base
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {

    /**
     * 知识库名称
     *
     * 例如：售后政策知识库、物流规则知识库
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识库状态
     *
     * ENABLED：启用
     * DISABLED：禁用
     */
    private String status;

    /**
     * 创建人ID
     */
    private Long createdBy;
}
