package com.ddy.aicustomerservice.module.document.service;

/**
 * @author 罗亚兰
 * @date 2026/5/19 9:46
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.document.dto.DocumentChunkPageQuery;
import com.ddy.aicustomerservice.module.document.vo.DocumentChunkResultVO;
import com.ddy.aicustomerservice.module.document.vo.DocumentChunkVO;

import java.util.List;

/**
 * 文档切片业务接口
 */
public interface DocumentChunkService {

    /**
     * 对单个文档进行切片
     */
    DocumentChunkResultVO chunkDocument(Long documentId);

    /**
     * 批量切分某个知识库下已解析文档
     */
    List<DocumentChunkResultVO> chunkParsedDocuments(Long knowledgeBaseId);

    /**
     * 分页查询文档切片
     */
    PageResult<DocumentChunkVO> pageChunks(DocumentChunkPageQuery query);

    /**
     * 查询某个文档的全部切片
     */
    List<DocumentChunkVO> listChunksByDocumentId(Long documentId);

    /**
     * 删除某个文档的全部切片
     */
    void deleteChunksByDocumentId(Long documentId);
}