package com.ddy.aicustomerservice.config;

import com.ddy.aicustomerservice.common.properties.VectorStoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;

/**
 * 本地 JSON 向量库（本机开发，零额外中间件）
 */
@Configuration
@RequiredArgsConstructor
public class SimpleVectorStoreConfig {

    private final VectorStoreProperties vectorStoreProperties;

    @Bean
    @Primary
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        File storeFile = new File(vectorStoreProperties.getSimpleStorePath());

        if (storeFile.exists() && storeFile.isFile()) {
            vectorStore.load(storeFile);
        }

        return vectorStore;
    }
}
