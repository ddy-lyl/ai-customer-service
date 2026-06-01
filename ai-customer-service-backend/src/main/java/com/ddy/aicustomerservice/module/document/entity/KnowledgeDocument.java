package com.ddy.aicustomerservice.module.document.entity;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:07
 **/
import com.baomidou.mybatisplus.annotation.TableName;
import com.ddy.aicustomerservice.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档实体类
 *
 * 对应数据库表：knowledge_document
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    /**
     * 所属知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 服务器存储文件名
     */
    private String storedFilename;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件类型
     *
     * pdf、docx、txt、md
     */
    private String fileType;

    /**
     * 文件大小，单位字节
     */
    private Long fileSize;

    /**
     * 文档处理状态
     *
     * UPLOADED、PARSING、PARSED、CHUNKED、VECTORIZED、FAILED
     */
    private String status;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 解析后的纯文本内容
     */
    private String parsedContent;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 上传人ID
     */
    private Long uploadedBy;
}