package com.ddy.aicustomerservice.common.properties;

/**
 * @author 罗亚兰
 * @date 2026/5/18 22:17
 **/
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件上传配置
 *
 * 对应 application.yml：
 * file:
 *   upload:
 *     knowledge-dir: ./uploads/knowledge
 */
@Data
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    /**
     * 知识库文件保存目录
     */
    private String knowledgeDir;
}