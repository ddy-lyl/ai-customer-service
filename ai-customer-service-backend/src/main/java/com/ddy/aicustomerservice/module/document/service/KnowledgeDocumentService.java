package com.ddy.aicustomerservice.module.document.service;

/**
 * @author 罗亚兰
 * @date 2026/5/18 22:22
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.document.dto.KnowledgeDocumentPageQuery;
import com.ddy.aicustomerservice.module.document.dto.KnowledgeDocumentStatusUpdateRequest;
import com.ddy.aicustomerservice.module.document.vo.DocumentParseResultVO;
import com.ddy.aicustomerservice.module.document.vo.KnowledgeDocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文档业务接口
 */
public interface KnowledgeDocumentService {

    /**
     * 上传知识库文档
     *
     * @param knowledgeBaseId 知识库ID
     * @param file 上传文件
     * @return 文档信息
     */
    KnowledgeDocumentVO uploadDocument(Long knowledgeBaseId, MultipartFile file);

    /**
     * 分页查询知识库文档
     */
    PageResult<KnowledgeDocumentVO> pageDocuments(KnowledgeDocumentPageQuery query);

    /**
     * 查看文档详情
     */
    KnowledgeDocumentVO getDocumentDetail(Long id);

    /**
     * 修改文档状态
     */
    void updateDocumentStatus(Long id, KnowledgeDocumentStatusUpdateRequest request);

    /**
     * 删除文档
     */
    void deleteDocument(Long id);

    /**
     * 解析单个文档
     */
    DocumentParseResultVO parseDocument(Long id);

    /**
     * 批量解析某个知识库下未解析的文档
     */
    List<DocumentParseResultVO> parseUnparsedDocuments(Long knowledgeBaseId);
}