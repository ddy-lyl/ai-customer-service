package com.ddy.aicustomerservice.module.document.service;

/**
 * @author 罗亚兰
 * @date 2026/5/19 8:50
 **/
import com.ddy.aicustomerservice.module.document.entity.KnowledgeDocument;

/**
 * 文档解析业务接口
 */
public interface DocumentParseService {

    /**
     * 解析文档，返回纯文本内容
     *
     * @param document 文档记录
     * @return 解析后的纯文本
     */
    String parseDocument(KnowledgeDocument document);
}
