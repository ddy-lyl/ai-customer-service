package com.ddy.aicustomerservice.module.document.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/19 11:22
 **/
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ddy.aicustomerservice.common.enums.DocumentStatusEnum;
import com.ddy.aicustomerservice.common.enums.KnowledgeBaseStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.properties.VectorStoreProperties;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.document.support.VectorStorePersistenceHelper;
import com.ddy.aicustomerservice.module.document.entity.DocumentChunk;
import com.ddy.aicustomerservice.module.document.entity.KnowledgeDocument;
import com.ddy.aicustomerservice.module.document.mapper.DocumentChunkMapper;
import com.ddy.aicustomerservice.module.document.mapper.KnowledgeDocumentMapper;
import com.ddy.aicustomerservice.module.document.service.DocumentVectorService;
import com.ddy.aicustomerservice.module.document.vo.DocumentVectorStatusVO;
import com.ddy.aicustomerservice.module.document.vo.DocumentVectorizeResultVO;
import com.ddy.aicustomerservice.module.knowledge.entity.KnowledgeBase;
import com.ddy.aicustomerservice.module.knowledge.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文档向量化业务实现类
 */
@Service
@RequiredArgsConstructor
public class DocumentVectorServiceImpl implements DocumentVectorService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    private final DocumentChunkMapper documentChunkMapper;

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    private final VectorStore vectorStore;

    private final VectorStoreProperties vectorStoreProperties;

    private final VectorStorePersistenceHelper vectorStorePersistenceHelper;

    /**
     * 向量化指定文档的未向量化切片
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVectorizeResultVO vectorizeDocument(Long documentId) {
        KnowledgeDocument document = getDocumentRequired(documentId);

        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(document.getKnowledgeBaseId());

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库未启用，不能向量化"
            );
        }

        validateDocumentCanVectorize(document);

        List<DocumentChunk> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .eq(DocumentChunk::getVectorized, 0)
                        .orderByAsc(DocumentChunk::getChunkIndex)
        );

        if (chunks.isEmpty()) {
            return buildVectorizeResult(document, true, 0, null);
        }

        try {
            List<Document> springAiDocuments = new ArrayList<>();

            for (DocumentChunk chunk : chunks) {
                if (!StringUtils.hasText(chunk.getContent())) {
                    continue;
                }

                String vectorId = buildVectorId(chunk.getId());

                Document springAiDocument = new Document(
                        vectorId,
                        chunk.getContent(),
                        buildMetadata(document, chunk)
                );

                springAiDocuments.add(springAiDocument);
            }

            if (springAiDocuments.isEmpty()) {
                throw new BusinessException(
                        ResultCodeEnum.PARAM_ERROR.getCode(),
                        "没有可向量化的切片内容"
                );
            }

            /*
             * 核心：写入向量库。
             * SimpleVectorStore 会调用 EmbeddingModel 把文本转成向量，再保存。
             */
            vectorStore.add(springAiDocuments);
            vectorStorePersistenceHelper.persistIfNeeded();

            /*
             * 更新 document_chunk 表。
             */
            for (DocumentChunk chunk : chunks) {
                if (!StringUtils.hasText(chunk.getContent())) {
                    continue;
                }

                chunk.setVectorId(buildVectorId(chunk.getId()));
                chunk.setEmbeddingModel(vectorStoreProperties.getEmbeddingModelName());
                chunk.setVectorized(1);

                documentChunkMapper.updateById(chunk);
            }

            /*
             * 如果该文档所有切片都已经向量化，就更新文档状态。
             */
            Long unVectorizedCount = countUnVectorizedChunks(documentId);

            if (unVectorizedCount == 0) {
                document.setStatus(DocumentStatusEnum.VECTORIZED.getCode());
                document.setFailReason(null);
                knowledgeDocumentMapper.updateById(document);
            }

            return buildVectorizeResult(document, true, springAiDocuments.size(), null);

        } catch (Exception e) {
            document.setStatus(DocumentStatusEnum.FAILED.getCode());
            document.setFailReason(e.getMessage());
            knowledgeDocumentMapper.updateById(document);

            return buildVectorizeResult(document, false, 0, e.getMessage());
        }
    }

    /**
     * 批量向量化某个知识库下已切片文档
     */
    @Override
    public List<DocumentVectorizeResultVO> vectorizeKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(knowledgeBaseId);

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库未启用，不能批量向量化"
            );
        }

        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeDocument::getStatus, DocumentStatusEnum.CHUNKED.getCode())
                        .orderByAsc(KnowledgeDocument::getCreateTime)
        );

        return documents.stream()
                .map(document -> vectorizeDocument(document.getId()))
                .toList();
    }

    /**
     * 删除某个文档的向量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocumentVectors(Long documentId) {
        KnowledgeDocument document = getDocumentRequired(documentId);

        List<DocumentChunk> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .eq(DocumentChunk::getVectorized, 1)
        );

        if (chunks.isEmpty()) {
            return;
        }

        List<String> vectorIds = chunks.stream()
                .map(DocumentChunk::getVectorId)
                .filter(StringUtils::hasText)
                .toList();

        if (!vectorIds.isEmpty()) {
            vectorStore.delete(vectorIds);
            vectorStorePersistenceHelper.persistIfNeeded();
        }

        for (DocumentChunk chunk : chunks) {
            chunk.setVectorId(null);
            chunk.setEmbeddingModel(null);
            chunk.setVectorized(0);

            documentChunkMapper.updateById(chunk);
        }

        document.setStatus(DocumentStatusEnum.CHUNKED.getCode());
        knowledgeDocumentMapper.updateById(document);
    }

    /**
     * 查询某个文档的向量化状态
     */
    @Override
    public DocumentVectorStatusVO getVectorStatus(Long documentId) {
        KnowledgeDocument document = getDocumentRequired(documentId);

        Long total = countChunks(documentId);
        Long vectorized = countVectorizedChunks(documentId);
        Long unVectorized = total - vectorized;

        DocumentVectorStatusVO vo = new DocumentVectorStatusVO();

        vo.setDocumentId(document.getId());
        vo.setOriginalFilename(document.getOriginalFilename());
        vo.setStatus(document.getStatus());
        vo.setStatusName(convertDocumentStatusName(document.getStatus()));
        vo.setTotalChunkCount(total);
        vo.setVectorizedChunkCount(vectorized);
        vo.setUnVectorizedChunkCount(unVectorized);
        vo.setProgressText(buildProgressText(total, vectorized));

        return vo;
    }

    /**
     * 校验文档是否可以向量化
     */
    private void validateDocumentCanVectorize(KnowledgeDocument document) {
        if (!DocumentStatusEnum.CHUNKED.getCode().equals(document.getStatus())
                && !DocumentStatusEnum.VECTORIZED.getCode().equals(document.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "只有 CHUNKED 或 VECTORIZED 状态的文档可以向量化"
            );
        }

        Long chunkCount = countChunks(document.getId());

        if (chunkCount == 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "该文档没有切片，请先进行文档切片"
            );
        }
    }

    /**
     * 构建向量库中的文档ID。
     *
     * 注意：
     * 这个 ID 必须稳定。
     * 后面删除向量时也靠它删除。
     */
    private String buildVectorId(Long chunkId) {
        return "chunk:" + chunkId;
    }

    /**
     * 构建 Spring AI Document 元数据。
     *
     * 元数据用于后续检索时知道这个向量来自哪个知识库、哪个文档、哪个切片。
     */
    private Map<String, Object> buildMetadata(KnowledgeDocument document, DocumentChunk chunk) {
        return Map.of(
                "knowledgeBaseId", chunk.getKnowledgeBaseId(),
                "documentId", chunk.getDocumentId(),
                "chunkId", chunk.getId(),
                "chunkIndex", chunk.getChunkIndex(),
                "originalFilename", document.getOriginalFilename()
        );
    }

    /**
     * 查询文档，不存在则抛异常
     */
    private KnowledgeDocument getDocumentRequired(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);

        if (document == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "文档不存在"
            );
        }

        return document;
    }

    /**
     * 查询知识库，不存在则抛异常
     */
    private KnowledgeBase getKnowledgeBaseRequired(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);

        if (knowledgeBase == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "知识库不存在"
            );
        }

        return knowledgeBase;
    }

    private Long countChunks(Long documentId) {
        return documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
        );
    }

    private Long countVectorizedChunks(Long documentId) {
        return documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .eq(DocumentChunk::getVectorized, 1)
        );
    }

    private Long countUnVectorizedChunks(Long documentId) {
        return documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .eq(DocumentChunk::getVectorized, 0)
        );
    }

    /**
     * 构建向量化结果
     */
    private DocumentVectorizeResultVO buildVectorizeResult(KnowledgeDocument document,
                                                           boolean success,
                                                           Integer vectorizedCount,
                                                           String failReason) {
        DocumentVectorizeResultVO vo = new DocumentVectorizeResultVO();

        vo.setDocumentId(document.getId());
        vo.setOriginalFilename(document.getOriginalFilename());
        vo.setSuccess(success);
        vo.setStatus(document.getStatus());
        vo.setStatusName(convertDocumentStatusName(document.getStatus()));
        vo.setTotalChunkCount(countChunks(document.getId()));
        vo.setVectorizedCount(vectorizedCount);
        vo.setTotalVectorizedCount(countVectorizedChunks(document.getId()));
        vo.setEmbeddingModelName(vectorStoreProperties.getEmbeddingModelName());
        vo.setFailReason(failReason);

        return vo;
    }

    private String buildProgressText(Long total, Long vectorized) {
        if (total == null || total == 0) {
            return "0%";
        }

        long percent = Math.round(vectorized * 100.0 / total);

        return percent + "%";
    }

    private String convertDocumentStatusName(String status) {
        if (status == null) {
            return null;
        }

        for (DocumentStatusEnum item : DocumentStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                return item.getName();
            }
        }

        return "未知状态";
    }
}
