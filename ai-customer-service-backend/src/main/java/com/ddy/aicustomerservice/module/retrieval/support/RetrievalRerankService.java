package com.ddy.aicustomerservice.module.retrieval.support;

import com.ddy.aicustomerservice.module.retrieval.vo.RetrievedChunkVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 检索结果重排序：向量粗召回 + 关键词 RRF 融合。
 * <p>
 * 不依赖外部 Rerank API，适合本地演示与 CI。
 */
@Component
public class RetrievalRerankService {

    private static final int RRF_K = 60;

    /**
     * 对候选切片重排并截断到 finalTopK，重写 rankNo 与 score（融合分）。
     */
    public List<RetrievedChunkVO> rerank(String question,
                                         List<RetrievedChunkVO> candidates,
                                         int finalTopK,
                                         double vectorRrfWeight) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        if (finalTopK < 1) {
            finalTopK = 1;
        }

        double vectorWeight = clamp(vectorRrfWeight, 0, 1);
        double keywordWeight = 1.0 - vectorWeight;

        Set<String> queryTerms = tokenize(question);

        List<ScoredCandidate> scored = new ArrayList<>(candidates.size());

        for (int i = 0; i < candidates.size(); i++) {
            RetrievedChunkVO chunk = candidates.get(i);
            int vectorRank = i + 1;
            double vectorRrf = 1.0 / (RRF_K + vectorRank);
            double keywordScore = keywordOverlapScore(queryTerms, chunk.getChunkContent());
            double fused = vectorWeight * vectorRrf + keywordWeight * keywordScore;
            scored.add(new ScoredCandidate(chunk, fused));
        }

        scored.sort(Comparator.comparingDouble(ScoredCandidate::fusedScore).reversed());

        List<RetrievedChunkVO> result = new ArrayList<>();
        int rank = 1;
        for (ScoredCandidate item : scored) {
            if (result.size() >= finalTopK) {
                break;
            }
            RetrievedChunkVO vo = item.chunk();
            vo.setRankNo(rank++);
            vo.setScore(item.fusedScore());
            result.add(vo);
        }

        return result;
    }

    public static Set<String> tokenize(String text) {
        Set<String> terms = new HashSet<>();
        if (!StringUtils.hasText(text)) {
            return terms;
        }
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ");
        for (String part : normalized.split("\\s+")) {
            if (part.length() >= 2) {
                terms.add(part);
            }
        }
        return terms;
    }

    public static double keywordOverlapScore(Set<String> queryTerms, String content) {
        if (queryTerms.isEmpty() || !StringUtils.hasText(content)) {
            return 0;
        }
        Set<String> docTerms = tokenize(content);
        if (docTerms.isEmpty()) {
            return 0;
        }
        int hit = 0;
        for (String term : queryTerms) {
            if (docTerms.contains(term)) {
                hit++;
            }
        }
        return (double) hit / queryTerms.size();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ScoredCandidate(RetrievedChunkVO chunk, double fusedScore) {
    }
}
