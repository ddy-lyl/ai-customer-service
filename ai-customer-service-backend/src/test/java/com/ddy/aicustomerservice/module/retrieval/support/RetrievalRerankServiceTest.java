package com.ddy.aicustomerservice.module.retrieval.support;

import com.ddy.aicustomerservice.module.retrieval.vo.RetrievedChunkVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalRerankServiceTest {

    private final RetrievalRerankService rerankService = new RetrievalRerankService();

    @Test
    void rerank_prefersKeywordMatch() {
        RetrievedChunkVO lowVectorHighKeyword = chunk(1L, "七天无理由退货需在签收后7天内申请");
        RetrievedChunkVO highVectorLowKeyword = chunk(2L, "物流配送时效说明");

        List<RetrievedChunkVO> result = rerankService.rerank(
                "七天无理由退货规则",
                List.of(highVectorLowKeyword, lowVectorHighKeyword),
                1,
                0.5
        );

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getChunkId());
    }

    @Test
    void tokenize_supportsChineseTerms() {
        var terms = RetrievalRerankService.tokenize("退货 退款政策");
        assertTrue(terms.contains("退货"));
        assertTrue(terms.contains("退款政策"));
    }

    private static RetrievedChunkVO chunk(Long id, String content) {
        RetrievedChunkVO vo = new RetrievedChunkVO();
        vo.setChunkId(id);
        vo.setChunkContent(content);
        vo.setRankNo(1);
        vo.setScore(0.9);
        return vo;
    }
}
