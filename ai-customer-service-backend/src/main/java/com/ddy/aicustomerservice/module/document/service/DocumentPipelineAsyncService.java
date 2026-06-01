package com.ddy.aicustomerservice.module.document.service;

/**
 * 文档上传后异步流水线：解析 → 切片 → 向量化
 */
public interface DocumentPipelineAsyncService {

    /**
     * 异步执行完整流水线（上传成功后触发）
     */
    void runPipelineAsync(Long documentId);
}
