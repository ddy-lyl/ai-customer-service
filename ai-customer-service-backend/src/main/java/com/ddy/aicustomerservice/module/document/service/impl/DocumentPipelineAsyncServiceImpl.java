package com.ddy.aicustomerservice.module.document.service.impl;

import com.ddy.aicustomerservice.module.document.service.DocumentChunkService;
import com.ddy.aicustomerservice.module.document.service.DocumentPipelineAsyncService;
import com.ddy.aicustomerservice.module.document.service.DocumentVectorService;
import com.ddy.aicustomerservice.module.document.service.KnowledgeDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 知识库文档一键处理流水线
 */
@Slf4j
@Service
public class DocumentPipelineAsyncServiceImpl implements DocumentPipelineAsyncService {

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final DocumentChunkService documentChunkService;

    private final DocumentVectorService documentVectorService;

    public DocumentPipelineAsyncServiceImpl(
            @Lazy KnowledgeDocumentService knowledgeDocumentService,
            DocumentChunkService documentChunkService,
            DocumentVectorService documentVectorService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.documentChunkService = documentChunkService;
        this.documentVectorService = documentVectorService;
    }

    @Override
    @Async
    public void runPipelineAsync(Long documentId) {
        log.info("[文档流水线] 开始 documentId={}", documentId);
        try {
            knowledgeDocumentService.parseDocument(documentId);
            documentChunkService.chunkDocument(documentId);
            documentVectorService.vectorizeDocument(documentId);
            log.info("[文档流水线] 完成 documentId={}", documentId);
        } catch (Exception e) {
            log.error("[文档流水线] 失败 documentId={}", documentId, e);
        }
    }
}
