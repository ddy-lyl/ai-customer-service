package com.ddy.aicustomerservice.module.document.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 9:44
 **/
import lombok.Data;

/**
 * 文档切片结果响应对象
 */
@Data
public class DocumentChunkResultVO {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 是否切片成功
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
     * 切片大小
     */
    private Integer chunkSize;

    /**
     * 切片重叠长度
     */
    private Integer chunkOverlap;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 失败原因
     */
    private String failReason;
}
