package com.ddy.aicustomerservice.common.enums;

import lombok.Getter;

/**
 * 文档处理状态枚举
 */
@Getter
public enum DocumentStatusEnum {

    UPLOADED("UPLOADED", "已上传"),

    PARSING("PARSING", "解析中"),

    PARSED("PARSED", "解析完成"),

    CHUNKED("CHUNKED", "切片完成"),

    VECTORIZED("VECTORIZED", "向量化完成"),

    FAILED("FAILED", "处理失败");

    private final String code;

    private final String name;

    DocumentStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
