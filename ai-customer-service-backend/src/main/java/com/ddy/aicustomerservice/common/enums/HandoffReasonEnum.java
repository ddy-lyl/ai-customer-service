package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 转人工触发原因
 */
@Getter
public enum HandoffReasonEnum {

    USER_REQUEST("USER_REQUEST", "用户主动转人工"),
    RAG_MISS("RAG_MISS", "知识库未命中"),
    RAG_LOW_SCORE("RAG_LOW_SCORE", "检索置信度过低"),
    AI_REFUSAL("AI_REFUSAL", "AI 表示无法回答"),
    AI_FAILURE("AI_FAILURE", "AI 服务异常"),
    REPEAT_MISS("REPEAT_MISS", "连续未命中");

    private final String code;

    private final String name;

    HandoffReasonEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
