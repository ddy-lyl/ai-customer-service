package com.ddy.aicustomerservice.module.document.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/18 22:26
 **/
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.document.dto.KnowledgeDocumentPageQuery;
import com.ddy.aicustomerservice.module.document.dto.KnowledgeDocumentStatusUpdateRequest;
import com.ddy.aicustomerservice.module.document.service.KnowledgeDocumentService;
import com.ddy.aicustomerservice.module.document.vo.KnowledgeDocumentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ddy.aicustomerservice.module.document.vo.DocumentParseResultVO;

import java.util.List;

/**
 * 知识库文档管理控制器
 *
 * 只有管理员可以访问。
 */
@RestController
@RequestMapping("/api/admin/knowledge-documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    /**
     * 上传知识库文档
     *
     * 请求类型：
     * multipart/form-data
     *
     * 参数：
     * knowledgeBaseId：知识库ID
     * file：上传文件
     */
    @PostMapping("/upload")
    public Result<KnowledgeDocumentVO> uploadDocument(@RequestParam Long knowledgeBaseId,
                                                      @RequestParam MultipartFile file) {
        KnowledgeDocumentVO document = knowledgeDocumentService.uploadDocument(
                knowledgeBaseId,
                file
        );

        return Result.success(document);
    }

    /**
     * 分页查询知识库文档
     *
     * 示例：
     * GET /api/admin/knowledge-documents/page?pageNo=1&pageSize=10&knowledgeBaseId=1
     */
    @GetMapping("/page")
    public Result<PageResult<KnowledgeDocumentVO>> pageDocuments(KnowledgeDocumentPageQuery query) {
        PageResult<KnowledgeDocumentVO> pageResult =
                knowledgeDocumentService.pageDocuments(query);

        return Result.success(pageResult);
    }

    /**
     * 查看文档详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeDocumentVO> getDocumentDetail(@PathVariable Long id) {
        KnowledgeDocumentVO document = knowledgeDocumentService.getDocumentDetail(id);
        return Result.success(document);
    }

    /**
     * 修改文档状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateDocumentStatus(@PathVariable Long id,
                                             @Valid @RequestBody KnowledgeDocumentStatusUpdateRequest request) {
        knowledgeDocumentService.updateDocumentStatus(id, request);
        return Result.success();
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        knowledgeDocumentService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 手动解析单个文档
     *
     * 示例：
     * POST /api/admin/knowledge-documents/1/parse
     */
    @PostMapping("/{id}/parse")
    public Result<DocumentParseResultVO> parseDocument(@PathVariable Long id) {
        DocumentParseResultVO result = knowledgeDocumentService.parseDocument(id);
        return Result.success(result);
    }

    /**
     * 批量解析某个知识库下未解析文档
     *
     * 示例：
     * POST /api/admin/knowledge-documents/parse-all?knowledgeBaseId=1
     */
    @PostMapping("/parse-all")
    public Result<List<DocumentParseResultVO>> parseUnparsedDocuments(@RequestParam Long knowledgeBaseId) {
        List<DocumentParseResultVO> result =
                knowledgeDocumentService.parseUnparsedDocuments(knowledgeBaseId);

        return Result.success(result);
    }
}
