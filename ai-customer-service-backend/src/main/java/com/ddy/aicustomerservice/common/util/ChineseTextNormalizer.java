package com.ddy.aicustomerservice.common.util;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.springframework.util.StringUtils;

/**
 * 将 AI 回复中的繁体字统一转为简体中文，避免简繁混用被用户误认为乱码。
 */
public final class ChineseTextNormalizer {

    private ChineseTextNormalizer() {
    }

    public static String toSimplified(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return ZhConverterUtil.toSimple(text);
    }
}
