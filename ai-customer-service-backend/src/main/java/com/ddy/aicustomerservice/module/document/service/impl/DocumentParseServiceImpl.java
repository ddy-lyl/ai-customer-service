package com.ddy.aicustomerservice.module.document.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/19 8:51
 **/
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.document.entity.KnowledgeDocument;
import com.ddy.aicustomerservice.module.document.service.DocumentParseService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;

/**
 * 文档解析业务实现类
 *
 * 使用 Apache Tika 统一解析 txt、md、pdf、docx 等文件。
 */
@Service
@RequiredArgsConstructor
public class DocumentParseServiceImpl implements DocumentParseService {

    /**
     * Tika 解析器
     *
     * 这里直接 new 一个即可。
     * 它会根据文件内容和后缀自动选择合适解析方式。
     */
    private final Tika tika = new Tika();

    /**
     * 解析文档
     */
    @Override
    public String parseDocument(KnowledgeDocument document) {
        if (document == null) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文档不能为空"
            );
        }

        if (!StringUtils.hasText(document.getFilePath())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文档文件路径为空"
            );
        }

        File file = new File(document.getFilePath());

        if (!file.exists()) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "本地文件不存在：" + document.getFilePath()
            );
        }

        if (!file.isFile()) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文件路径不是有效文件：" + document.getFilePath()
            );
        }

        try {
            String content = tika.parseToString(file);

            return cleanText(content);
        } catch (Exception e) {
            throw new BusinessException(
                    ResultCodeEnum.FAIL.getCode(),
                    "文档解析失败：" + e.getMessage()
            );
        }
    }

    /**
     * 清洗文本
     *
     * 作用：
     * 1. 去掉过多空白
     * 2. 统一换行
     * 3. 避免 parsed_content 保存大量无意义空行
     */
    private String cleanText(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }

        String cleaned = content
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        // 每一行 trim
        String[] lines = cleaned.split("\n");

        StringBuilder builder = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();

            if (StringUtils.hasText(trimmed)) {
                builder.append(trimmed).append("\n");
            }
        }

        return builder.toString().trim();
    }
}