package com.ddy.aicustomerservice.module.document.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/19 11:24
 **/

import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.document.service.DocumentVectorService;
import com.ddy.aicustomerservice.module.document.vo.DocumentVectorStatusVO;
import com.ddy.aicustomerservice.module.document.vo.DocumentVectorizeResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

/**
 * 文档向量化管理控制器
 *
 * 只有管理员可以访问。
 */
@RestController
@RequestMapping("/api/admin/document-vectors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DocumentVectorController {

    private final DocumentVectorService documentVectorService;

    /**
     * 向量化指定文档
     *
     * 示例：
     * POST /api/admin/document-vectors/document/7/vectorize
     */
    @PostMapping("/document/{documentId}/vectorize")
    public Result<DocumentVectorizeResultVO> vectorizeDocument(@PathVariable Long documentId) {
        DocumentVectorizeResultVO result = documentVectorService.vectorizeDocument(documentId);
        return Result.success(result);
    }

    /**
     * 批量向量化某个知识库下已切片文档
     *
     * 示例：
     * POST /api/admin/document-vectors/vectorize-all?knowledgeBaseId=1
     */
    @PostMapping("/vectorize-all")
    public Result<List<DocumentVectorizeResultVO>> vectorizeKnowledgeBase(@RequestParam Long knowledgeBaseId) {
        List<DocumentVectorizeResultVO> results =
                documentVectorService.vectorizeKnowledgeBase(knowledgeBaseId);

        return Result.success(results);
    }

    /**
     * 删除某个文档的向量
     *
     * 示例：
     * DELETE /api/admin/document-vectors/document/7
     */
    @DeleteMapping("/document/{documentId}")
    public Result<Void> deleteDocumentVectors(@PathVariable Long documentId) {
        documentVectorService.deleteDocumentVectors(documentId);
        return Result.success();
    }

    /**
     * 查询某个文档的向量化状态
     *
     * 示例：
     * GET /api/admin/document-vectors/status/7
     */
    @GetMapping("/status/{documentId}")
    public Result<DocumentVectorStatusVO> getVectorStatus(@PathVariable Long documentId) {
        DocumentVectorStatusVO status = documentVectorService.getVectorStatus(documentId);
        return Result.success(status);
    }
}
