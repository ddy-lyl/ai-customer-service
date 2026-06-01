package com.ddy.aicustomerservice.module.document.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 11:21
 **/
import lombok.Data;

/**
 * 文档向量化状态响应对象
 */
@Data
public class DocumentVectorStatusVO {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文档状态
     */
    private String status;

    /**
     * 文档状态名称
     */
    private String statusName;

    /**
     * 切片总数
     */
    private Long totalChunkCount;

    /**
     * 已向量化切片数量
     */
    private Long vectorizedChunkCount;

    /**
     * 未向量化切片数量
     */
    private Long unVectorizedChunkCount;

    /**
     * 向量化进度百分比
     */
    private String progressText;
}