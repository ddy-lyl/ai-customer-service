package com.ddy.aicustomerservice.module.document.support;

/**
 * @author 罗亚兰
 * @date 2026/5/19 9:45
 **/
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档文本切片工具
 *
 * 策略：
 * 1. 固定最大长度
 * 2. 保留一定 overlap
 * 3. 尽量在自然边界切开
 */
public class DocumentTextChunker {

    private DocumentTextChunker() {
    }

    /**
     * 文本切片
     *
     * @param text 原始文本
     * @param chunkSize 每片最大长度
     * @param chunkOverlap 相邻切片重叠长度
     * @return 切片列表
     */
    public static List<String> chunk(String text, int chunkSize, int chunkOverlap) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        if (chunkSize <= 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "chunkSize 必须大于 0"
            );
        }

        if (chunkOverlap < 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "chunkOverlap 不能小于 0"
            );
        }

        if (chunkOverlap >= chunkSize) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "chunkOverlap 必须小于 chunkSize"
            );
        }

        String normalizedText = normalizeText(text);

        if (normalizedText.length() <= chunkSize) {
            return List.of(normalizedText);
        }

        List<String> chunks = new ArrayList<>();

        int start = 0;
        int textLength = normalizedText.length();

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            /*
             * 如果还没到全文末尾，就尽量找自然切分点。
             */
            if (end < textLength) {
                int betterEnd = findBetterEnd(normalizedText, start, end);

                if (betterEnd > start) {
                    end = betterEnd;
                }
            }

            String chunk = normalizedText.substring(start, end).trim();

            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }

            if (end >= textLength) {
                break;
            }

            /*
             * 下一片从 end - overlap 开始。
             * 同时防止 start 不前进导致死循环。
             */
            int nextStart = end - chunkOverlap;

            if (nextStart <= start) {
                nextStart = start + 1;
            }

            start = nextStart;
        }

        return chunks;
    }

    /**
     * 规范化文本
     */
    private static String normalizeText(String text) {
        String normalized = text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\t", " ");

        /*
         * 多个空格压缩成一个空格。
         * 多个连续空行压缩成两个换行。
         */
        normalized = normalized.replaceAll("[ ]{2,}", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");

        return normalized.trim();
    }

    /**
     * 尽量寻找自然切分位置。
     *
     * 优先级：
     * 1. 换行
     * 2. 中文句号、问号、叹号、分号
     * 3. 英文句号、问号、叹号、分号
     */
    private static int findBetterEnd(String text, int start, int maxEnd) {
        int minEnd = start + (maxEnd - start) / 2;

        for (int i = maxEnd - 1; i >= minEnd; i--) {
            char c = text.charAt(i);

            if (isBoundaryChar(c)) {
                return i + 1;
            }
        }

        return maxEnd;
    }

    /**
     * 判断是否是适合切开的字符
     */
    private static boolean isBoundaryChar(char c) {
        return c == '\n'
                || c == '。'
                || c == '？'
                || c == '！'
                || c == '；'
                || c == '.'
                || c == '?'
                || c == '!'
                || c == ';';
    }
}