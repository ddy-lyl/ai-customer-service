package com.ddy.aicustomerservice.module.retrieval.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/19 14:24
 **/

import com.ddy.aicustomerservice.common.constant.AiConstants;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.properties.RetrievalProperties;
import com.ddy.aicustomerservice.common.enums.DocumentStatusEnum;
import com.ddy.aicustomerservice.common.enums.KnowledgeBaseStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.document.entity.DocumentChunk;
import com.ddy.aicustomerservice.module.document.entity.KnowledgeDocument;
import com.ddy.aicustomerservice.module.document.mapper.DocumentChunkMapper;
import com.ddy.aicustomerservice.module.document.mapper.KnowledgeDocumentMapper;
import com.ddy.aicustomerservice.module.knowledge.entity.KnowledgeBase;
import com.ddy.aicustomerservice.module.knowledge.mapper.KnowledgeBaseMapper;
import com.ddy.aicustomerservice.module.retrieval.dto.KnowledgeRetrievalRequest;
import com.ddy.aicustomerservice.module.retrieval.entity.AiRetrievalLog;
import com.ddy.aicustomerservice.module.retrieval.mapper.AiRetrievalLogMapper;
import com.ddy.aicustomerservice.module.retrieval.service.KnowledgeRetrievalService;
import com.ddy.aicustomerservice.module.retrieval.support.RetrievalRerankService;
import com.ddy.aicustomerservice.module.retrieval.vo.KnowledgeRetrievalResultVO;
import com.ddy.aicustomerservice.module.retrieval.vo.RetrievedChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库向量检索业务实现类
 */
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final VectorStore vectorStore;

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    private final DocumentChunkMapper documentChunkMapper;

    private final AiRetrievalLogMapper aiRetrievalLogMapper;

    private final RetrievalProperties retrievalProperties;

    private final RetrievalRerankService retrievalRerankService;

    /**
     * 执行知识库向量检索
     *
     * sessionId / userMessageId 允许为 null，便于区分「AI 对话」与「管理员调试检索」。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeRetrievalResultVO retrieve(KnowledgeRetrievalRequest request) {
        Long sessionId = request.getSessionId();
        Long userMessageId = request.getUserMessageId();

        Long currentUserId = LoginUserContext.getUserId();

        if (currentUserId == null) {
            throw new BusinessException(
                    ResultCodeEnum.UNAUTHORIZED.getCode(),
                    "请先登录"
            );
        }

        int finalTopK = resolveTopK(request.getTopK());
        double similarityThreshold = resolveSimilarityThreshold(request.getSimilarityThreshold());

        int searchTopK = finalTopK;
        double searchThreshold = similarityThreshold;

        if (retrievalProperties.isRerankEnabled()) {
            searchTopK = resolveCoarseTopK(finalTopK);
            searchThreshold = resolveCoarseThreshold(similarityThreshold);
        }

        validateKnowledgeBaseIfSpecified(request.getKnowledgeBaseId());

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(request.getQuestionText())
                .topK(searchTopK)
                .similarityThreshold(searchThreshold);

        /**
         * 指定 knowledgeBaseId 时，在向量库层做过滤。
         * 不指定时先不加 in 表达式，避免 SimpleVectorStore 某些版本对 in 支持不稳定。
         * 后面会在 MySQL 回查阶段过滤 ENABLED 知识库。
         */
        if (request.getKnowledgeBaseId() != null) {
            builder.filterExpression("knowledgeBaseId == " + request.getKnowledgeBaseId());
        }

        List<Document> springAiDocuments = vectorStore.similaritySearch(builder.build());

        List<RetrievedChunkVO> retrievedChunks = new ArrayList<>();

        int rankNo = 1;

        if (springAiDocuments != null) {
            for (Document springAiDocument : springAiDocuments) {
                RetrievedChunkVO chunkVO = convertToRetrievedChunkVO(springAiDocument, rankNo);

                if (chunkVO == null) {
                    continue;
                }

                /**
                 * 如果请求指定了 knowledgeBaseId，只返回该知识库结果。
                 * 如果请求没指定，就只返回启用知识库结果。
                 */
                if (request.getKnowledgeBaseId() != null
                        && !request.getKnowledgeBaseId().equals(chunkVO.getKnowledgeBaseId())) {
                    continue;
                }

                retrievedChunks.add(chunkVO);
                rankNo++;
            }
        }

        if (retrievalProperties.isRerankEnabled() && !retrievedChunks.isEmpty()) {
            retrievedChunks = retrievalRerankService.rerank(
                    request.getQuestionText(),
                    retrievedChunks,
                    finalTopK,
                    retrievalProperties.getVectorRrfWeight()
            );
        } else if (retrievedChunks.size() > finalTopK) {
            retrievedChunks = new ArrayList<>(retrievedChunks.subList(0, finalTopK));
            for (int i = 0; i < retrievedChunks.size(); i++) {
                retrievedChunks.get(i).setRankNo(i + 1);
            }
        }

        /**
         * 记录召回日志：
         * 每一个召回切片插入一条 ai_retrieval_log。
         * 如果没有召回，也插入一条 rankNo = 0 的记录，表示本次问题没有召回结果。
         */
        saveRetrievalLogs(
                sessionId,
                currentUserId,
                userMessageId,
                request.getQuestionText(),
                request.getKnowledgeBaseId(),
                retrievedChunks
        );

        KnowledgeRetrievalResultVO result = new KnowledgeRetrievalResultVO();

        result.setSessionId(sessionId);
        result.setUserMessageId(userMessageId);
        result.setQuestionText(request.getQuestionText());
        result.setKnowledgeBaseId(request.getKnowledgeBaseId());
        result.setTopK(finalTopK);
        result.setSimilarityThreshold(similarityThreshold);
        result.setRerankEnabled(retrievalProperties.isRerankEnabled());
        result.setRetrievedCount(retrievedChunks.size());
        result.setChunks(retrievedChunks);
        result.setContextText(buildContextText(retrievedChunks));

        return result;
    }

    private int resolveCoarseTopK(int finalTopK) {
        int coarse = finalTopK * Math.max(1, retrievalProperties.getCoarseTopKMultiplier());
        return Math.min(coarse, Math.max(1, retrievalProperties.getMaxCoarseTopK()));
    }

    private double resolveCoarseThreshold(double finalThreshold) {
        double delta = retrievalProperties.getCoarseThresholdDelta();
        return Math.max(0, finalThreshold - delta);
    }

    /**
     * 解析 topK
     */
    private int resolveTopK(Integer topK) {
        if (topK == null) {
            return AiConstants.DEFAULT_TOP_K;
        }

        if (topK < 1) {
            return 1;
        }

        return Math.min(topK, 20);
    }

    /**
     * 解析相似度阈值
     */
    private double resolveSimilarityThreshold(Double similarityThreshold) {
        if (similarityThreshold == null) {
            return AiConstants.DEFAULT_SIMILARITY_THRESHOLD;
        }

        if (similarityThreshold < 0) {
            return 0;
        }

        if (similarityThreshold > 1) {
            return 1;
        }

        return similarityThreshold;
    }

    /**
     * 如果指定知识库，则校验知识库存在且启用
     */
    private void validateKnowledgeBaseIfSpecified(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return;
        }

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);

        if (knowledgeBase == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "知识库不存在"
            );
        }

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库未启用，不能检索"
            );
        }
    }

    /**
     * Spring AI Document 转换成业务召回 VO
     */
    private RetrievedChunkVO convertToRetrievedChunkVO(Document springAiDocument, Integer rankNo) {
        Long chunkId = extractChunkId(springAiDocument);

        if (chunkId == null) {
            return null;
        }

        DocumentChunk chunk = documentChunkMapper.selectById(chunkId);

        if (chunk == null) {
            return null;
        }

        if (chunk.getVectorized() == null || chunk.getVectorized() != 1) {
            return null;
        }

        KnowledgeDocument document = knowledgeDocumentMapper.selectById(chunk.getDocumentId());

        if (document == null) {
            return null;
        }

        if (!DocumentStatusEnum.VECTORIZED.getCode().equals(document.getStatus())) {
            return null;
        }

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(chunk.getKnowledgeBaseId());

        if (knowledgeBase == null) {
            return null;
        }

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            return null;
        }

        RetrievedChunkVO vo = new RetrievedChunkVO();

        vo.setRankNo(rankNo);
        vo.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
        vo.setKnowledgeBaseName(knowledgeBase.getName());
        vo.setDocumentId(document.getId());
        vo.setDocumentName(document.getOriginalFilename());
        vo.setChunkId(chunk.getId());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setChunkContent(chunk.getContent());
        vo.setChunkContentPreview(buildContentPreview(chunk.getContent()));
        vo.setScore(springAiDocument.getScore());
        vo.setVectorId(chunk.getVectorId());
        vo.setEmbeddingModel(chunk.getEmbeddingModel());
        vo.setContentHash(chunk.getContentHash());
        vo.setTokenCount(chunk.getTokenCount());

        return vo;
    }

    /**
     * 从 Spring AI Document 中提取 chunkId
     *
     * 优先从 metadata.chunkId 取。
     * 如果 metadata 丢失，再从 document.id = chunk:9 里解析。
     */
    private Long extractChunkId(Document springAiDocument) {
        Map<String, Object> metadata = springAiDocument.getMetadata();

        if (metadata != null) {
            Long chunkId = toLong(metadata.get("chunkId"));

            if (chunkId != null) {
                return chunkId;
            }
        }

        String documentId = springAiDocument.getId();

        if (!StringUtils.hasText(documentId)) {
            return null;
        }

        if (documentId.startsWith("chunk:")) {
            String idText = documentId.substring("chunk:".length());

            try {
                return Long.parseLong(idText);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Object 转 Long
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
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
     * 构建 RAG 上下文文本
     *
     * 第 18 章 AI 问答会使用这个 contextText。
     */
    private String buildContextText(List<RetrievedChunkVO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (RetrievedChunkVO chunk : chunks) {
            builder.append("[来源").append(chunk.getRankNo()).append("]\n");
            builder.append("知识库：").append(chunk.getKnowledgeBaseName()).append("\n");
            builder.append("文档：").append(chunk.getDocumentName()).append("\n");
            builder.append("切片ID：").append(chunk.getChunkId()).append("\n");
            builder.append("相似度：").append(chunk.getScore()).append("\n");
            builder.append("内容：").append(chunk.getChunkContent()).append("\n\n");
        }

        return builder.toString().trim();
    }

    /**
     * 保存召回日志
     *
     * 一条召回切片 = 一条 ai_retrieval_log。
     */
    private void saveRetrievalLogs(Long sessionId,
                                   Long userId,
                                   Long userMessageId,
                                   String questionText,
                                   Long requestKnowledgeBaseId,
                                   List<RetrievedChunkVO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            AiRetrievalLog log = new AiRetrievalLog();

            log.setSessionId(sessionId);
            log.setUserId(userId);
            log.setUserMessageId(userMessageId);
            log.setQuestionText(questionText);
            log.setKnowledgeBaseId(requestKnowledgeBaseId);
            log.setDocumentId(null);
            log.setDocumentName(null);
            log.setChunkId(null);
            log.setChunkContent(null);
            log.setScore(null);
            log.setRankNo(0);

            aiRetrievalLogMapper.insert(log);
            return;
        }

        for (RetrievedChunkVO chunk : chunks) {
            AiRetrievalLog log = new AiRetrievalLog();

            log.setSessionId(sessionId);
            log.setUserId(userId);
            log.setUserMessageId(userMessageId);
            log.setQuestionText(questionText);
            log.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
            log.setDocumentId(chunk.getDocumentId());
            log.setDocumentName(chunk.getDocumentName());
            log.setChunkId(chunk.getChunkId());
            log.setChunkContent(chunk.getChunkContent());
            log.setScore(chunk.getScore());
            log.setRankNo(chunk.getRankNo());

            aiRetrievalLogMapper.insert(log);
        }
    }
}
