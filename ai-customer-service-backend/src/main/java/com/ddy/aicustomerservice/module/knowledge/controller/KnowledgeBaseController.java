package com.ddy.aicustomerservice.module.knowledge.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:14
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseCreateRequest;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBasePageQuery;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseStatusUpdateRequest;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.ddy.aicustomerservice.module.knowledge.service.KnowledgeBaseService;
import com.ddy.aicustomerservice.module.knowledge.vo.KnowledgeBaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理控制器
 *
 * 只有管理员可以访问。
 */
@RestController
@RequestMapping("/api/admin/knowledge-bases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 新增知识库
     */
    @PostMapping
    public Result<KnowledgeBaseVO> createKnowledgeBase(
            @Valid @RequestBody KnowledgeBaseCreateRequest request
    ) {
        KnowledgeBaseVO knowledgeBase = knowledgeBaseService.createKnowledgeBase(request);
        return Result.success(knowledgeBase);
    }

    /**
     * 分页查询知识库
     *
     * 示例：
     * GET /api/admin/knowledge-bases/page?pageNo=1&pageSize=10&name=售后&status=ENABLED
     */
    @GetMapping("/page")
    public Result<PageResult<KnowledgeBaseVO>> pageKnowledgeBases(KnowledgeBasePageQuery query) {
        PageResult<KnowledgeBaseVO> pageResult = knowledgeBaseService.pageKnowledgeBases(query);
        return Result.success(pageResult);
    }

    /**
     * 查询启用的知识库
     *
     * 后面文档上传、AI 对话选择知识库时会用。
     */
    @GetMapping("/enabled")
    public Result<List<KnowledgeBaseVO>> listEnabledKnowledgeBases() {
        List<KnowledgeBaseVO> list = knowledgeBaseService.listEnabledKnowledgeBases();
        return Result.success(list);
    }

    /**
     * 查看知识库详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getKnowledgeBaseDetail(@PathVariable Long id) {
        KnowledgeBaseVO knowledgeBase = knowledgeBaseService.getKnowledgeBaseDetail(id);
        return Result.success(knowledgeBase);
    }

    /**
     * 修改知识库
     */
    @PutMapping("/{id}")
    public Result<KnowledgeBaseVO> updateKnowledgeBase(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseUpdateRequest request
    ) {
        KnowledgeBaseVO knowledgeBase = knowledgeBaseService.updateKnowledgeBase(id, request);
        return Result.success(knowledgeBase);
    }

    /**
     * 启用 / 禁用知识库
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateKnowledgeBaseStatus(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseStatusUpdateRequest request
    ) {
        knowledgeBaseService.updateKnowledgeBaseStatus(id, request);
        return Result.success();
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Result.success();
    }
}