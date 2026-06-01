package com.ddy.aicustomerservice.module.document.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 8:49
 **/
import lombok.Data;

/**
 * 文档解析结果响应对象
 */
@Data
public class DocumentParseResultVO {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 是否解析成功
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
     * 解析后的文本长度
     */
    private Integer contentLength;

    /**
     * 解析后的文本预览
     */
    private String contentPreview;

    /**
     * 失败原因
     */
    private String failReason;
}