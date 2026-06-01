package com.ddy.aicustomerservice.module.chat.support;

import com.ddy.aicustomerservice.common.enums.HandoffReasonEnum;
import com.ddy.aicustomerservice.config.AiCustomerServiceProperties;
import com.ddy.aicustomerservice.module.retrieval.vo.KnowledgeRetrievalResultVO;
import com.ddy.aicustomerservice.module.retrieval.vo.RetrievedChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 判断是否应自动转人工
 */
@Component
@RequiredArgsConstructor
public class HandoffTriggerEvaluator {

    private final AiCustomerServiceProperties properties;

    public HandoffReasonEnum evaluate(String questionText,
                                      KnowledgeRetrievalResultVO retrieval,
                                      String answer,
                                      int recentRagMissCount) {
        if (!Boolean.TRUE.equals(properties.getHandoff().getEnabled())) {
            return null;
        }

        if (matchesUserRequest(questionText)) {
            return HandoffReasonEnum.USER_REQUEST;
        }

        if (isAiFailureAnswer(answer)) {
            return HandoffReasonEnum.AI_FAILURE;
        }

        if (matchesRefusal(answer)) {
            return HandoffReasonEnum.AI_REFUSAL;
        }

        int retrieved = retrieval != null && retrieval.getRetrievedCount() != null
                ? retrieval.getRetrievedCount() : 0;

        if (Boolean.TRUE.equals(properties.getHandoff().getRagMissAuto())) {
            if (retrieved == 0 && !isSmallTalk(questionText)) {
                if (recentRagMissCount >= safeRepeatMissCount()) {
                    return HandoffReasonEnum.REPEAT_MISS;
                }
                return HandoffReasonEnum.RAG_MISS;
            }

            Double maxScore = resolveMaxScore(retrieval);
            Double threshold = properties.getHandoff().getLowScoreThreshold();
            if (retrieved > 0 && maxScore != null && threshold != null && maxScore < threshold) {
                return HandoffReasonEnum.RAG_LOW_SCORE;
            }
        }

        return null;
    }

    public boolean matchesUserRequest(String questionText) {
        if (!StringUtils.hasText(questionText)) {
            return false;
        }
        String text = questionText.trim();
        for (String keyword : splitKeywords(properties.getHandoff().getUserKeywords())) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAiFailureAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return true;
        }
        return answer.contains("AI 服务暂时不可用")
                || answer.contains("没有生成有效回答");
    }

    private boolean matchesRefusal(String answer) {
        if (!StringUtils.hasText(answer)) {
            return false;
        }
        for (String phrase : splitKeywords(properties.getHandoff().getRefusalPhrases())) {
            if (answer.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSmallTalk(String questionText) {
        if (!StringUtils.hasText(questionText)) {
            return true;
        }
        String t = questionText.trim();
        return t.length() <= 8
                && (t.contains("你好") || t.contains("谢谢") || t.contains("再见") || t.equals("hi"));
    }

    private Double resolveMaxScore(KnowledgeRetrievalResultVO retrieval) {
        if (retrieval == null || retrieval.getChunks() == null || retrieval.getChunks().isEmpty()) {
            return null;
        }
        return retrieval.getChunks().stream()
                .map(RetrievedChunkVO::getScore)
                .filter(score -> score != null)
                .max(Double::compareTo)
                .orElse(null);
    }

    private int safeRepeatMissCount() {
        Integer count = properties.getHandoff().getRepeatMissCount();
        return count == null || count < 2 ? 2 : count;
    }

    private List<String> splitKeywords(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
