package com.ddy.aicustomerservice.module.retrieval.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/19 14:26
 **/
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.retrieval.dto.KnowledgeRetrievalRequest;
import com.ddy.aicustomerservice.module.retrieval.service.KnowledgeRetrievalService;
import com.ddy.aicustomerservice.module.retrieval.vo.KnowledgeRetrievalResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库向量检索控制器
 *
 * 当前阶段先给管理员测试 RAG 检索效果。
 */
@RestController
@RequestMapping("/api/admin/knowledge-retrieval")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeRetrievalController {

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    /**
     * 执行知识库向量检索
     */
    @PostMapping("/search")
    public Result<KnowledgeRetrievalResultVO> search(
            @Valid @RequestBody KnowledgeRetrievalRequest request
    ) {
        KnowledgeRetrievalResultVO result = knowledgeRetrievalService.retrieve(request);
        return Result.success(result);
    }
}
