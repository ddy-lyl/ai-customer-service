package com.ddy.aicustomerservice.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 向量库存储配置（本地 JSON SimpleVectorStore）
 *
 * 对应 application.yml:
 *
 * vector:
 *   store:
 *     simple-store-path: ./uploads/vector-store/knowledge-vector-store.json
 *     embedding-model-name: text-embedding-v3
 */
@Data
@ConfigurationProperties(prefix = "vector.store")
public class VectorStoreProperties {

    /**
     * SimpleVectorStore 本地持久化文件路径
     */
    private String simpleStorePath;

    /**
     * 当前使用的向量模型名称（与 spring.ai.openai.embedding 配置一致）
     */
    private String embeddingModelName;
}
