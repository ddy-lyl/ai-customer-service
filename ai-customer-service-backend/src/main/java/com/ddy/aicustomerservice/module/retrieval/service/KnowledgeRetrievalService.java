package com.ddy.aicustomerservice.module.retrieval.service;

/**
 * @author 罗亚兰
 * @date 2026/5/19 14:23
 **/
import com.ddy.aicustomerservice.module.retrieval.dto.KnowledgeRetrievalRequest;
import com.ddy.aicustomerservice.module.retrieval.vo.KnowledgeRetrievalResultVO;

/**
 * 知识库向量检索业务接口
 */
public interface KnowledgeRetrievalService {

    /**
     * 执行知识库向量检索
     */
    KnowledgeRetrievalResultVO retrieve(KnowledgeRetrievalRequest request);
}
