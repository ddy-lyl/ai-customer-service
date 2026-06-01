package com.ddy.aicustomerservice.module.knowledge.service;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:12
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseCreateRequest;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBasePageQuery;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseStatusUpdateRequest;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.ddy.aicustomerservice.module.knowledge.vo.KnowledgeBaseVO;

import java.util.List;

/**
 * 知识库业务接口
 */
public interface KnowledgeBaseService {

    /**
     * 新增知识库
     */
    KnowledgeBaseVO createKnowledgeBase(KnowledgeBaseCreateRequest request);

    /**
     * 分页查询知识库
     */
    PageResult<KnowledgeBaseVO> pageKnowledgeBases(KnowledgeBasePageQuery query);

    /**
     * 查询启用的知识库
     *
     * 后面 AI 对话和文档上传会用到。
     */
    List<KnowledgeBaseVO> listEnabledKnowledgeBases();

    /**
     * 查看知识库详情
     */
    KnowledgeBaseVO getKnowledgeBaseDetail(Long id);

    /**
     * 修改知识库
     */
    KnowledgeBaseVO updateKnowledgeBase(Long id, KnowledgeBaseUpdateRequest request);

    /**
     * 修改知识库状态
     */
    void updateKnowledgeBaseStatus(Long id, KnowledgeBaseStatusUpdateRequest request);

    /**
     * 删除知识库
     */
    void deleteKnowledgeBase(Long id);
}
