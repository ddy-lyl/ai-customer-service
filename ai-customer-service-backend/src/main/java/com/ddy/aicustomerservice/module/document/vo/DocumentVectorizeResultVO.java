package com.ddy.aicustomerservice.module.document.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 11:20
 **/
import lombok.Data;

/**
 * 文档向量化结果响应对象
 */
@Data
public class DocumentVectorizeResultVO {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 文档状态
     */
    private String status;

    /**
     * 文档状态名称
     */
    private String statusName;

    /**
     * 总切片数量
     */
    private Long totalChunkCount;

    /**
     * 本次向量化切片数量
     */
    private Integer vectorizedCount;

    /**
     * 已向量化切片总数
     */
    private Long totalVectorizedCount;

    /**
     * 向量模型名称
     */
    private String embeddingModelName;

    /**
     * 失败原因
     */
    private String failReason;
}
