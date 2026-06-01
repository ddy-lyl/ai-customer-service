package com.ddy.aicustomerservice.module.document.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/18 22:22
 **/
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.enums.DocumentStatusEnum;
import com.ddy.aicustomerservice.common.enums.KnowledgeBaseStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.properties.FileUploadProperties;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.document.dto.KnowledgeDocumentPageQuery;
import com.ddy.aicustomerservice.module.document.dto.KnowledgeDocumentStatusUpdateRequest;
import com.ddy.aicustomerservice.module.document.entity.DocumentChunk;
import com.ddy.aicustomerservice.module.document.entity.KnowledgeDocument;
import com.ddy.aicustomerservice.module.document.mapper.DocumentChunkMapper;
import com.ddy.aicustomerservice.module.document.mapper.KnowledgeDocumentMapper;
import com.ddy.aicustomerservice.module.document.service.DocumentParseService;
import com.ddy.aicustomerservice.module.document.service.DocumentPipelineAsyncService;
import com.ddy.aicustomerservice.module.document.service.DocumentVectorService;
import com.ddy.aicustomerservice.module.document.service.KnowledgeDocumentService;
import com.ddy.aicustomerservice.module.document.vo.DocumentParseResultVO;
import com.ddy.aicustomerservice.module.document.vo.KnowledgeDocumentVO;
import com.ddy.aicustomerservice.module.knowledge.entity.KnowledgeBase;
import com.ddy.aicustomerservice.module.knowledge.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库文档业务实现类
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    private final DocumentChunkMapper documentChunkMapper;

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    private final FileUploadProperties fileUploadProperties;

    private final DocumentParseService documentParseService;

    private final DocumentPipelineAsyncService documentPipelineAsyncService;

    private final DocumentVectorService documentVectorService;

    /**
     * 允许上传的文件类型
     */
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "txt",
            "md",
            "pdf",
            "docx"
    );

    /**
     * 上传知识库文档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO uploadDocument(Long knowledgeBaseId, MultipartFile file) {
        // 1. 校验知识库
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(knowledgeBaseId);

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库未启用，不能上传文档"
            );
        }

        // 2. 校验文件
        validateFile(file);

        // 3. 获取原始文件名
        String originalFilename = file.getOriginalFilename();

        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文件名不能为空"
            );
        }

        // 4. 获取文件后缀
        String fileType = getFileType(originalFilename);

        // 5. 生成服务器存储文件名
        String storedFilename = generateStoredFilename(fileType);

        // 6. 构建保存目录
        Path uploadDir = Paths.get(fileUploadProperties.getKnowledgeDir())
                .toAbsolutePath()
                .normalize();

        // 7. 如果目录不存在，则创建目录
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new BusinessException("创建文件上传目录失败");
        }

        // 8. 构建完整保存路径
        Path targetPath = uploadDir.resolve(storedFilename);

        // 9. 保存文件到本地
        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }

        // 10. 保存文档记录到数据库
        KnowledgeDocument document = new KnowledgeDocument();

        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setOriginalFilename(originalFilename);
        document.setStoredFilename(storedFilename);
        document.setFilePath(targetPath.toString());
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setStatus(DocumentStatusEnum.UPLOADED.getCode());
        document.setChunkCount(0);
        document.setParsedContent(null);
        document.setFailReason(null);
        document.setUploadedBy(LoginUserContext.getUserId());

        knowledgeDocumentMapper.insert(document);

        documentPipelineAsyncService.runPipelineAsync(document.getId());

        return convertToVO(document);
    }

    /**
     * 分页查询文档
     */
    @Override
    public PageResult<KnowledgeDocumentVO> pageDocuments(KnowledgeDocumentPageQuery query) {
        Page<KnowledgeDocument> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(query.getKnowledgeBaseId() != null,
                KnowledgeDocument::getKnowledgeBaseId,
                query.getKnowledgeBaseId());

        wrapper.like(StringUtils.hasText(query.getOriginalFilename()),
                KnowledgeDocument::getOriginalFilename,
                query.getOriginalFilename());

        wrapper.eq(StringUtils.hasText(query.getFileType()),
                KnowledgeDocument::getFileType,
                query.getFileType());

        wrapper.eq(StringUtils.hasText(query.getStatus()),
                KnowledgeDocument::getStatus,
                query.getStatus());

        wrapper.eq(query.getUploadedBy() != null,
                KnowledgeDocument::getUploadedBy,
                query.getUploadedBy());

        wrapper.orderByDesc(KnowledgeDocument::getCreateTime);

        Page<KnowledgeDocument> documentPage =
                knowledgeDocumentMapper.selectPage(page, wrapper);

        return PageResult.of(
                documentPage.getCurrent(),
                documentPage.getSize(),
                documentPage.getTotal(),
                documentPage.getPages(),
                documentPage.getRecords()
                        .stream()
                        .map(this::convertToVO)
                        .toList()
        );
    }

    /**
     * 查看文档详情
     */
    @Override
    public KnowledgeDocumentVO getDocumentDetail(Long id) {
        KnowledgeDocument document = getDocumentRequired(id);
        return convertToVO(document);
    }

    /**
     * 修改文档状态
     */
    @Override
    public void updateDocumentStatus(Long id, KnowledgeDocumentStatusUpdateRequest request) {
        KnowledgeDocument document = getDocumentRequired(id);

        if (!isValidDocumentStatus(request.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文档状态不合法"
            );
        }

        document.setStatus(request.getStatus());

        if (DocumentStatusEnum.FAILED.getCode().equals(request.getStatus())) {
            document.setFailReason(request.getFailReason());
        } else {
            document.setFailReason(null);
        }

        knowledgeDocumentMapper.updateById(document);
    }

    /**
     * 删除文档
     *
     * 删除文档时要同时处理：
     * 1. 删除向量库中的向量（若已向量化）
     * 2. 删除 document_chunk 切片记录
     * 3. 删除 knowledge_document 文档记录
     * 4. 删除本地文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        KnowledgeDocument document = getDocumentRequired(id);

        documentVectorService.deleteDocumentVectors(id);

        documentChunkMapper.delete(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, id)
        );

        knowledgeDocumentMapper.deleteById(id);

        deleteLocalFile(document.getFilePath());
    }

    /**
     * 解析单个文档
     *
     * 状态变化：
     * UPLOADED / FAILED / PARSED
     *      ↓
     * PARSING
     *      ↓
     * PARSED 或 FAILED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentParseResultVO parseDocument(Long id) {
        KnowledgeDocument document = getDocumentRequired(id);

        // 1. 校验知识库
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(document.getKnowledgeBaseId());

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库未启用，不能解析文档"
            );
        }

        // 2. 已经向量化的文档不建议重新解析
        if (DocumentStatusEnum.VECTORIZED.getCode().equals(document.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文档已向量化，不建议重新解析"
            );
        }

        // 3. 先更新为解析中
        document.setStatus(DocumentStatusEnum.PARSING.getCode());
        document.setFailReason(null);
        knowledgeDocumentMapper.updateById(document);

        try {
            // 4. 调用解析服务提取文本
            String parsedContent = documentParseService.parseDocument(document);

            if (!StringUtils.hasText(parsedContent)) {
                throw new BusinessException(
                        ResultCodeEnum.FAIL.getCode(),
                        "文档解析结果为空"
                );
            }

            // 5. 保存解析结果
            document.setParsedContent(parsedContent);
            document.setStatus(DocumentStatusEnum.PARSED.getCode());
            document.setFailReason(null);

            knowledgeDocumentMapper.updateById(document);

            return buildParseResult(document, true, null);

        } catch (Exception e) {
            // 6. 失败时记录失败原因
            String failReason = e.getMessage();

            document.setStatus(DocumentStatusEnum.FAILED.getCode());
            document.setFailReason(failReason);

            knowledgeDocumentMapper.updateById(document);

            return buildParseResult(document, false, failReason);
        }
    }

    /**
     * 批量解析某个知识库下未解析文档
     *
     * 只处理这些状态：
     * UPLOADED
     * FAILED
     *
     * 不处理：
     * PARSING、PARSED、CHUNKED、VECTORIZED
     */
    @Override
    public List<DocumentParseResultVO> parseUnparsedDocuments(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(knowledgeBaseId);

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(knowledgeBase.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库未启用，不能批量解析文档"
            );
        }

        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                        .in(KnowledgeDocument::getStatus,
                                DocumentStatusEnum.UPLOADED.getCode(),
                                DocumentStatusEnum.FAILED.getCode())
                        .orderByAsc(KnowledgeDocument::getCreateTime)
        );

        return documents.stream()
                .map(document -> parseDocument(document.getId()))
                .toList();
    }

    /**
     * 构建解析结果
     */
    private DocumentParseResultVO buildParseResult(KnowledgeDocument document,
                                                   boolean success,
                                                   String failReason) {
        DocumentParseResultVO vo = new DocumentParseResultVO();

        vo.setDocumentId(document.getId());
        vo.setOriginalFilename(document.getOriginalFilename());
        vo.setFileType(document.getFileType());
        vo.setSuccess(success);
        vo.setStatus(document.getStatus());
        vo.setStatusName(convertDocumentStatusName(document.getStatus()));

        String parsedContent = document.getParsedContent();

        int contentLength = parsedContent == null ? 0 : parsedContent.length();

        vo.setContentLength(contentLength);
        vo.setContentPreview(buildContentPreview(parsedContent));
        vo.setFailReason(failReason);

        return vo;
    }

    /**
     * 构建文本预览
     */
    private String buildContentPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        if (content.length() <= 200) {
            return content;
        }

        return content.substring(0, 200) + "...";
    }

    /**
     * 校验上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "上传文件不能为空"
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文件名不能为空"
            );
        }

        String fileType = getFileType(originalFilename);

        if (!ALLOWED_FILE_TYPES.contains(fileType)) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "暂只支持 txt、md、pdf、docx 文件"
            );
        }
    }

    /**
     * 根据文件名获取后缀
     */
    private String getFileType(String filename) {
        int index = filename.lastIndexOf(".");

        if (index < 0 || index == filename.length() - 1) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "文件缺少扩展名"
            );
        }

        return filename.substring(index + 1).toLowerCase();
    }

    /**
     * 生成服务器存储文件名
     *
     * 例如：
     * 1f2a3b4c5d6e7f8g.txt
     */
    private String generateStoredFilename(String fileType) {
        return UUID.randomUUID().toString().replace("-", "") + "." + fileType;
    }

    /**
     * 查询知识库，不存在则抛异常
     */
    private KnowledgeBase getKnowledgeBaseRequired(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);

        if (knowledgeBase == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "知识库不存在"
            );
        }

        return knowledgeBase;
    }

    /**
     * 查询文档，不存在则抛异常
     */
    private KnowledgeDocument getDocumentRequired(Long id) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(id);

        if (document == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "文档不存在"
            );
        }

        return document;
    }

    /**
     * 删除本地文件
     */
    private void deleteLocalFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            /*
             * 这里不抛异常。
             * 原因：
             * 数据库记录已经删除，如果本地文件删除失败，可以后续通过日志或清理任务处理。
             */
            e.printStackTrace();
        }
    }

    /**
     * 判断文档状态是否合法
     */
    private boolean isValidDocumentStatus(String status) {
        for (DocumentStatusEnum item : DocumentStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Entity 转 VO
     */
    private KnowledgeDocumentVO convertToVO(KnowledgeDocument document) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(document.getKnowledgeBaseId());

        vo.setId(document.getId());
        vo.setKnowledgeBaseId(document.getKnowledgeBaseId());
        vo.setKnowledgeBaseName(knowledgeBase == null ? null : knowledgeBase.getName());
        vo.setOriginalFilename(document.getOriginalFilename());
        vo.setFileType(document.getFileType());
        vo.setFileSize(document.getFileSize());
        vo.setFileSizeText(formatFileSize(document.getFileSize()));
        vo.setStatus(document.getStatus());
        vo.setStatusName(convertDocumentStatusName(document.getStatus()));
        vo.setChunkCount(document.getChunkCount());
        vo.setParsed(StringUtils.hasText(document.getParsedContent()));
        vo.setParsedContentLength(
                document.getParsedContent() == null ? 0 : document.getParsedContent().length()
        );
        vo.setFailReason(document.getFailReason());
        vo.setUploadedBy(document.getUploadedBy());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());

        return vo;
    }

    /**
     * 文档状态转中文名称
     */
    private String convertDocumentStatusName(String status) {
        if (status == null) {
            return null;
        }

        for (DocumentStatusEnum item : DocumentStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                return item.getName();
            }
        }

        return "未知状态";
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null) {
            return "0 B";
        }

        if (size < 1024) {
            return size + " B";
        }

        BigDecimal kb = BigDecimal.valueOf(size)
                .divide(BigDecimal.valueOf(1024), 2, RoundingMode.HALF_UP);

        if (kb.compareTo(BigDecimal.valueOf(1024)) < 0) {
            return kb + " KB";
        }

        BigDecimal mb = kb.divide(BigDecimal.valueOf(1024), 2, RoundingMode.HALF_UP);

        return mb + " MB";
    }
}