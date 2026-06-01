package com.ddy.aicustomerservice.module.document.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/19 9:50
 **/

import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.Result;
import com.ddy.aicustomerservice.module.document.dto.DocumentChunkPageQuery;
import com.ddy.aicustomerservice.module.document.service.DocumentChunkService;
import com.ddy.aicustomerservice.module.document.vo.DocumentChunkResultVO;
import com.ddy.aicustomerservice.module.document.vo.DocumentChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档切片管理控制器
 *
 * 只有管理员可以访问。
 */
@RestController
@RequestMapping("/api/admin/document-chunks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DocumentChunkController {

    private final DocumentChunkService documentChunkService;

    /**
     * 对单个文档进行切片
     *
     * 示例：
     * POST /api/admin/document-chunks/7/chunk
     */
    @PostMapping("/{documentId}/chunk")
    public Result<DocumentChunkResultVO> chunkDocument(@PathVariable Long documentId) {
        DocumentChunkResultVO result = documentChunkService.chunkDocument(documentId);
        return Result.success(result);
    }

    /**
     * 批量切分某个知识库下已解析文档
     *
     * 示例：
     * POST /api/admin/document-chunks/chunk-all?knowledgeBaseId=1
     */
    @PostMapping("/chunk-all")
    public Result<List<DocumentChunkResultVO>> chunkParsedDocuments(@RequestParam Long knowledgeBaseId) {
        List<DocumentChunkResultVO> results =
                documentChunkService.chunkParsedDocuments(knowledgeBaseId);

        return Result.success(results);
    }

    /**
     * 分页查询文档切片
     *
     * 示例：
     * GET /api/admin/document-chunks/page?pageNo=1&pageSize=10&documentId=7
     */
    @GetMapping("/page")
    public Result<PageResult<DocumentChunkVO>> pageChunks(DocumentChunkPageQuery query) {
        PageResult<DocumentChunkVO> pageResult = documentChunkService.pageChunks(query);
        return Result.success(pageResult);
    }

    /**
     * 查询某个文档的全部切片
     *
     * 示例：
     * GET /api/admin/document-chunks/document/7
     */
    @GetMapping("/document/{documentId}")
    public Result<List<DocumentChunkVO>> listChunksByDocumentId(@PathVariable Long documentId) {
        List<DocumentChunkVO> chunks = documentChunkService.listChunksByDocumentId(documentId);
        return Result.success(chunks);
    }

    /**
     * 删除某个文档的全部切片
     *
     * 示例：
     * DELETE /api/admin/document-chunks/document/7
     */
    @DeleteMapping("/document/{documentId}")
    public Result<Void> deleteChunksByDocumentId(@PathVariable Long documentId) {
        documentChunkService.deleteChunksByDocumentId(documentId);
        return Result.success();
    }
}
