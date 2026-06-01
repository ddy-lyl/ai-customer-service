package com.ddy.aicustomerservice.module.knowledge.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:12
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库响应对象
 */
@Data
public class KnowledgeBaseVO {

    /**
     * 知识库ID
     */
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 状态编码：
     * ENABLED / DISABLED
     */
    private String status;

    /**
     * 状态名称：
     * 启用 / 禁用
     */
    private String statusName;

    /**
     * 文档数量
     */
    private Long documentCount;

    /**
     * 切片数量
     */
    private Long chunkCount;

    /**
     * 已向量化切片数量
     */
    private Long vectorizedChunkCount;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
