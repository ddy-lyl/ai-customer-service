package com.ddy.aicustomerservice.module.document.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/18 22:20
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档响应对象
 */
@Data
public class KnowledgeDocumentVO {

    /**
     * 文档ID
     */
    private Long id;

    /**
     * 所属知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 所属知识库名称
     */
    private String knowledgeBaseName;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小，单位字节
     */
    private Long fileSize;

    /**
     * 文件大小展示文本
     *
     * 例如：12.5 KB、3.2 MB
     */
    private String fileSizeText;

    /**
     * 文档处理状态
     */
    private String status;

    /**
     * 文档处理状态名称
     */
    private String statusName;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 是否已经解析出文本
     */
    private Boolean parsed;

    /**
     * 解析后的纯文本长度
     */
    private Integer parsedContentLength;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 上传人ID
     */
    private Long uploadedBy;

    /**
     * 上传时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}