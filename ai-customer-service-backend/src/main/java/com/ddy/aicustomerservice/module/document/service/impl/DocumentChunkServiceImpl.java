package com.ddy.aicustomerservice.module.document.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/19 9:46
 **/
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.constant.AiConstants;
import com.ddy.aicustomerservice.common.enums.DocumentStatusEnum;
import com.ddy.aicustomerservice.common.enums.KnowledgeBaseStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.document.dto.DocumentChunkPageQuery;
import com.ddy.aicustomerservice.module.document.entity.DocumentChunk;
import com.ddy.aicustomerservice.module.document.entity.KnowledgeDocument;
import com.ddy.aicustomerservice.module.document.mapper.DocumentChunkMapper;
import com.ddy.aicustomerservice.module.document.mapper.KnowledgeDocumentMapper;
import com.ddy.aicustomerservice.module.document.service.DocumentChunkService;
import com.ddy.aicustomerservice.module.document.support.DocumentTextChunker;
import com.ddy.aicustomerservice.module.document.vo.DocumentChunkResultVO;
import com.ddy.aicustomerservice.module.document.vo.DocumentChunkVO;
import com.ddy.aicustomerservice.module.knowledge.entity.KnowledgeBase;
import com.ddy.aicustomerservice.module.knowledge.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文档切片业务实现类
 */
@Service
@RequiredArgsConstructor
public class DocumentChunkServiceImpl implements DocumentChunkService {

    private final DocumentChunkMapper documentChunkMapper;

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 对单个文档进行切片
     *
     * 状态变化：
     * PARSED -> CHUNKED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentChunkResultVO chunkDocument(Long documentId) {
        KnowledgeDocument document = getDocumentRequired(documentId);

        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(document.getKnowledgeBaseId());

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库未启用，不能切片"
            );
        }

        validateDocumentCanChunk(document);

        try {
            // 1. 先删除旧切片，避免重复切片
            deleteOldChunks(document.getId());

            // 2. 读取解析后的纯文本
            String parsedContent = document.getParsedContent();

            // 3. 执行文本切片
            List<String> chunkTexts = DocumentTextChunker.chunk(
                    parsedContent,
                    AiConstants.DEFAULT_CHUNK_SIZE,
                    AiConstants.DEFAULT_CHUNK_OVERLAP
            );

            if (chunkTexts.isEmpty()) {
                throw new BusinessException(
                        ResultCodeEnum.FAIL.getCode(),
                        "文档切片结果为空"
                );
            }

            // 4. 保存切片
            for (int i = 0; i < chunkTexts.size(); i++) {
                String chunkText = chunkTexts.get(i);

                DocumentChunk chunk = new DocumentChunk();
                chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunkText);
                chunk.setContentHash(calculateContentHash(chunkText));
                chunk.setTokenCount(estimateTokenCount(chunkText));
                chunk.setVectorId(null);
                chunk.setEmbeddingModel(null);
                chunk.setVectorized(0);
                chunk.setMetadataJson(buildMetadataJson(document, i));

                documentChunkMapper.insert(chunk);
            }

            // 5. 更新文档状态
            document.setChunkCount(chunkTexts.size());
            document.setStatus(DocumentStatusEnum.CHUNKED.getCode());
            document.setFailReason(null);

            knowledgeDocumentMapper.updateById(document);

            return buildChunkResult(document, true, null);

        } catch (Exception e) {
            document.setStatus(DocumentStatusEnum.FAILED.getCode());
            document.setFailReason(e.getMessage());

            knowledgeDocumentMapper.updateById(document);

            return buildChunkResult(document, false, e.getMessage());
        }
    }

    /**
     * 批量切分某个知识库下已解析文档
     *
     * 只处理 PARSED 状态的文档。
     */
    @Override
    public List<DocumentChunkResultVO> chunkParsedDocuments(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(knowledgeBaseId);

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库未启用，不能批量切片"
            );
        }

        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeDocument::getStatus, DocumentStatusEnum.PARSED.getCode())
                        .orderByAsc(KnowledgeDocument::getCreateTime)
        );

        return documents.stream()
                .map(document -> chunkDocument(document.getId()))
                .toList();
    }

    /**
     * 分页查询切片
     */
    @Override
    public PageResult<DocumentChunkVO> pageChunks(DocumentChunkPageQuery query) {
        Page<DocumentChunk> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(query.getKnowledgeBaseId() != null,
                DocumentChunk::getKnowledgeBaseId,
                query.getKnowledgeBaseId());

        wrapper.eq(query.getDocumentId() != null,
                DocumentChunk::getDocumentId,
                query.getDocumentId());

        wrapper.like(StringUtils.hasText(query.getContentKeyword()),
                DocumentChunk::getContent,
                query.getContentKeyword());

        wrapper.eq(query.getVectorized() != null,
                DocumentChunk::getVectorized,
                query.getVectorized());

        wrapper.orderByAsc(DocumentChunk::getDocumentId);
        wrapper.orderByAsc(DocumentChunk::getChunkIndex);

        Page<DocumentChunk> chunkPage = documentChunkMapper.selectPage(page, wrapper);

        List<DocumentChunkVO> records = chunkPage.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(
                chunkPage.getCurrent(),
                chunkPage.getSize(),
                chunkPage.getTotal(),
                chunkPage.getPages(),
                records
        );
    }

    /**
     * 查询某个文档的全部切片
     */
    @Override
    public List<DocumentChunkVO> listChunksByDocumentId(Long documentId) {
        getDocumentRequired(documentId);

        List<DocumentChunk> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .orderByAsc(DocumentChunk::getChunkIndex)
        );

        return chunks.stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 删除某个文档的全部切片
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChunksByDocumentId(Long documentId) {
        KnowledgeDocument document = getDocumentRequired(documentId);

        Long vectorizedCount = documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .eq(DocumentChunk::getVectorized, 1)
        );

        if (vectorizedCount > 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "该文档已有向量化切片，不能直接删除切片"
            );
        }

        documentChunkMapper.delete(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
        );

        document.setChunkCount(0);

        if (StringUtils.hasText(document.getParsedContent())) {
            document.setStatus(DocumentStatusEnum.PARSED.getCode());
        }

        knowledgeDocumentMapper.updateById(document);
    }

    /**
     * 校验文档是否可以切片
     */
    private void validateDocumentCanChunk(KnowledgeDocument document) {
        if (DocumentStatusEnum.VECTORIZED.getCode().equals(document.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文档已向量化，不能重新切片"
            );
        }

        if (!DocumentStatusEnum.PARSED.getCode().equals(document.getStatus())
                && !DocumentStatusEnum.CHUNKED.getCode().equals(document.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "只有 PARSED 或 CHUNKED 状态的文档可以切片"
            );
        }

        if (!StringUtils.hasText(document.getParsedContent())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文档解析内容为空，请先解析文档"
            );
        }
    }

    /**
     * 删除旧切片
     */
    private void deleteOldChunks(Long documentId) {
        documentChunkMapper.delete(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
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

    /**
     * 计算内容哈希
     *
     * 作用：
     * 后面可以用于切片去重。
     */
    private String calculateContentHash(String content) {
        return DigestUtils.md5DigestAsHex(
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 粗略估算 token 数量
     *
     * 注意：
     * 这里只是估算，不是精确 token 计算。
     */
    private Integer estimateTokenCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }

        return Math.max(1, (int) Math.ceil(content.length() / 2.0));
    }

    /**
     * 构建元数据 JSON
     */
    private String buildMetadataJson(KnowledgeDocument document, Integer chunkIndex) {
        return """
                {"documentId":%d,"chunkIndex":%d,"originalFilename":"%s"}
                """.formatted(
                document.getId(),
                chunkIndex,
                escapeJson(document.getOriginalFilename())
        ).trim();
    }

    /**
     * 简单处理 JSON 字符串中的双引号
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\"", "\\\"");
    }

    /**
     * 构建切片结果
     */
    private DocumentChunkResultVO buildChunkResult(KnowledgeDocument document,
                                                   boolean success,
                                                   String failReason) {
        DocumentChunkResultVO vo = new DocumentChunkResultVO();

        vo.setDocumentId(document.getId());
        vo.setOriginalFilename(document.getOriginalFilename());
        vo.setSuccess(success);
        vo.setStatus(document.getStatus());
        vo.setStatusName(convertDocumentStatusName(document.getStatus()));
        vo.setChunkSize(AiConstants.DEFAULT_CHUNK_SIZE);
        vo.setChunkOverlap(AiConstants.DEFAULT_CHUNK_OVERLAP);
        vo.setChunkCount(document.getChunkCount());
        vo.setFailReason(failReason);

        return vo;
    }

    /**
     * Entity 转 VO
     */
    private DocumentChunkVO convertToVO(DocumentChunk chunk) {
        DocumentChunkVO vo = new DocumentChunkVO();

        KnowledgeDocument document = knowledgeDocumentMapper.selectById(chunk.getDocumentId());
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(chunk.getKnowledgeBaseId());

        vo.setId(chunk.getId());
        vo.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
        vo.setKnowledgeBaseName(knowledgeBase == null ? null : knowledgeBase.getName());
        vo.setDocumentId(chunk.getDocumentId());
        vo.setOriginalFilename(document == null ? null : document.getOriginalFilename());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent());
        vo.setContentPreview(buildContentPreview(chunk.getContent()));
        vo.setContentHash(chunk.getContentHash());
        vo.setTokenCount(chunk.getTokenCount());
        vo.setVectorId(chunk.getVectorId());
        vo.setEmbeddingModel(chunk.getEmbeddingModel());
        vo.setVectorized(chunk.getVectorized());
        vo.setVectorizedName(convertVectorizedName(chunk.getVectorized()));
        vo.setMetadataJson(chunk.getMetadataJson());
        vo.setCreateTime(chunk.getCreateTime());
        vo.setUpdateTime(chunk.getUpdateTime());

        return vo;
    }

    /**
     * 构建内容预览
     */
    private String buildContentPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        if (content.length() <= 120) {
            return content;
        }

        return content.substring(0, 120) + "...";
    }

    /**
     * 文档状态转中文名称
     */
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

    /**
     * 向量化状态转中文
     */
    private String convertVectorizedName(Integer vectorized) {
        if (vectorized == null || vectorized == 0) {
            return "未向量化";
        }

        if (vectorized == 1) {
            return "已向量化";
        }

        return "未知";
    }
}
