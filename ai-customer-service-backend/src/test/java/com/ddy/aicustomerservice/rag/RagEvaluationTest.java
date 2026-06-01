package com.ddy.aicustomerservice.rag;

import com.ddy.aicustomerservice.module.retrieval.support.RetrievalRerankService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RAG 评测：基于关键词命中（不依赖外部大模型 API，CI 可跳过 integration 标签外的用例）。
 * <p>
 * 完整向量 Recall@K 需在本地向量化知识库后，使用 profile=integration 运行。
 */
class RagEvaluationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void evalQuestions_fileLoads() throws Exception {
        List<RagEvalQuestion> questions = loadQuestions();
        assertTrue(questions.size() >= 5, "评测集至少 5 条");
    }

    @Test
    void keywordOverlap_detectsExpectedTerms() throws Exception {
        List<RagEvalQuestion> questions = loadQuestions();
        int hit = 0;
        for (RagEvalQuestion q : questions) {
            Set<String> queryTerms = RetrievalRerankService.tokenize(q.question());
            double score = RetrievalRerankService.keywordOverlapScore(queryTerms, q.sampleAnswerSnippet());
            if (score >= 0.3) {
                hit++;
            }
        }
        assertTrue(hit >= questions.size() / 2, "关键词评测命中率应 >= 50%");
    }

    @Tag("integration")
    @Test
    void integration_placeholder_requiresEmbeddingApi() {
        // 本地执行：mvn test -Dgroups=integration
        // 对接 KnowledgeRetrievalService 计算 Recall@K
    }

    private List<RagEvalQuestion> loadQuestions() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/rag-eval/questions.json")) {
            if (in == null) {
                throw new IllegalStateException("缺少 rag-eval/questions.json");
            }
            return objectMapper.readValue(in, new TypeReference<>() {
            });
        }
    }

    record RagEvalQuestion(
            String id,
            String question,
            List<String> expectedKeywords,
            String sampleAnswerSnippet
    ) {
    }
}
