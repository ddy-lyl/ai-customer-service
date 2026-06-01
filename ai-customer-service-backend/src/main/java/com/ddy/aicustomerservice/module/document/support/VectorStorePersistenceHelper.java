package com.ddy.aicustomerservice.module.document.support;

import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.properties.VectorStoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * SimpleVectorStore 落盘到本地 JSON 文件。
 */
@Component
@RequiredArgsConstructor
public class VectorStorePersistenceHelper {

    private final VectorStore vectorStore;

    private final VectorStoreProperties vectorStoreProperties;

    public void persistIfNeeded() {
        if (!(vectorStore instanceof SimpleVectorStore simpleVectorStore)) {
            return;
        }

        File storeFile = new File(vectorStoreProperties.getSimpleStorePath());
        File parentFile = storeFile.getParentFile();

        if (parentFile != null && !parentFile.exists()) {
            boolean created = parentFile.mkdirs();
            if (!created && !parentFile.exists()) {
                throw new BusinessException("创建向量库目录失败");
            }
        }

        simpleVectorStore.save(storeFile);
    }
}
