package com.ddy.aicustomerservice.module.document.service;

/**
 * @author 罗亚兰
 * @date 2026/5/19 11:21
 **/
import com.ddy.aicustomerservice.module.document.vo.DocumentVectorStatusVO;
import com.ddy.aicustomerservice.module.document.vo.DocumentVectorizeResultVO;

import java.util.List;

/**
 * 文档向量化业务接口
 */
public interface DocumentVectorService {

    /**
     * 向量化指定文档的未向量化切片
     */
    DocumentVectorizeResultVO vectorizeDocument(Long documentId);

    /**
     * 批量向量化某个知识库下的已切片文档
     */
    List<DocumentVectorizeResultVO> vectorizeKnowledgeBase(Long knowledgeBaseId);

    /**
     * 删除某个文档的向量
     */
    void deleteDocumentVectors(Long documentId);

    /**
     * 查询某个文档的向量化状态
     */
    DocumentVectorStatusVO getVectorStatus(Long documentId);
}