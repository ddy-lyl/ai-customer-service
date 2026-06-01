package com.ddy.aicustomerservice.module.knowledge.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:13
 **/
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.enums.KnowledgeBaseStatusEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.document.entity.DocumentChunk;
import com.ddy.aicustomerservice.module.document.entity.KnowledgeDocument;
import com.ddy.aicustomerservice.module.document.mapper.DocumentChunkMapper;
import com.ddy.aicustomerservice.module.document.mapper.KnowledgeDocumentMapper;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseCreateRequest;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBasePageQuery;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseStatusUpdateRequest;
import com.ddy.aicustomerservice.module.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.ddy.aicustomerservice.module.knowledge.entity.KnowledgeBase;
import com.ddy.aicustomerservice.module.knowledge.mapper.KnowledgeBaseMapper;
import com.ddy.aicustomerservice.module.knowledge.service.KnowledgeBaseService;
import com.ddy.aicustomerservice.module.knowledge.vo.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 知识库业务实现类
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    private final DocumentChunkMapper documentChunkMapper;

    /**
     * 新增知识库
     */
    @Override
    public KnowledgeBaseVO createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        checkNameUnique(request.getName(), null);

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setStatus(KnowledgeBaseStatusEnum.ENABLED.getCode());
        knowledgeBase.setCreatedBy(LoginUserContext.getUserId());

        knowledgeBaseMapper.insert(knowledgeBase);

        return convertToVO(knowledgeBase);
    }

    /**
     * 分页查询知识库
     */
    @Override
    public PageResult<KnowledgeBaseVO> pageKnowledgeBases(KnowledgeBasePageQuery query) {
        Page<KnowledgeBase> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(query.getName()),
                KnowledgeBase::getName,
                query.getName());

        wrapper.eq(StringUtils.hasText(query.getStatus()),
                KnowledgeBase::getStatus,
                query.getStatus());

        wrapper.orderByDesc(KnowledgeBase::getCreateTime);

        Page<KnowledgeBase> knowledgeBasePage =
                knowledgeBaseMapper.selectPage(page, wrapper);

        List<KnowledgeBaseVO> records = knowledgeBasePage.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(
                knowledgeBasePage.getCurrent(),
                knowledgeBasePage.getSize(),
                knowledgeBasePage.getTotal(),
                knowledgeBasePage.getPages(),
                records
        );
    }

    /**
     * 查询启用的知识库
     */
    @Override
    public List<KnowledgeBaseVO> listEnabledKnowledgeBases() {
        List<KnowledgeBase> list = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getStatus, KnowledgeBaseStatusEnum.ENABLED.getCode())
                        .orderByDesc(KnowledgeBase::getCreateTime)
        );

        return list.stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 查看知识库详情
     */
    @Override
    public KnowledgeBaseVO getKnowledgeBaseDetail(Long id) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(id);
        return convertToVO(knowledgeBase);
    }

    /**
     * 修改知识库
     */
    @Override
    public KnowledgeBaseVO updateKnowledgeBase(Long id, KnowledgeBaseUpdateRequest request) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(id);

        checkNameUnique(request.getName(), id);

        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());

        knowledgeBaseMapper.updateById(knowledgeBase);

        return convertToVO(knowledgeBase);
    }

    /**
     * 修改知识库状态
     */
    @Override
    public void updateKnowledgeBaseStatus(Long id, KnowledgeBaseStatusUpdateRequest request) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(id);

        if (!KnowledgeBaseStatusEnum.ENABLED.getCode().equals(request.getStatus())
                && !KnowledgeBaseStatusEnum.DISABLED.getCode().equals(request.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库状态只能是 ENABLED 或 DISABLED"
            );
        }

        knowledgeBase.setStatus(request.getStatus());

        knowledgeBaseMapper.updateById(knowledgeBase);
    }

    /**
     * 删除知识库
     *
     * 当前策略：
     * 如果知识库下还有文档，不允许删除。
     * 推荐先禁用知识库，或先删除文档。
     */
    @Override
    public void deleteKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseRequired(id);

        Long documentCount = countDocuments(id);

        if (documentCount > 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "该知识库下还有文档，不能直接删除，请先删除文档或禁用知识库"
            );
        }

        knowledgeBaseMapper.deleteById(knowledgeBase.getId());
    }

    /**
     * 查询知识库，不存在则抛异常
     */
    private KnowledgeBase getKnowledgeBaseRequired(Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);

        if (knowledgeBase == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "知识库不存在"
            );
        }

        return knowledgeBase;
    }

    /**
     * 校验知识库名称唯一
     *
     * @param name 知识库名称
     * @param excludeId 修改时排除当前记录ID；新增时传 null
     */
    private void checkNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(KnowledgeBase::getName, name);

        if (excludeId != null) {
            wrapper.ne(KnowledgeBase::getId, excludeId);
        }

        Long count = knowledgeBaseMapper.selectCount(wrapper);

        if (count > 0) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "知识库名称已存在"
            );
        }
    }

    /**
     * Entity 转 VO
     */
    private KnowledgeBaseVO convertToVO(KnowledgeBase knowledgeBase) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();

        vo.setId(knowledgeBase.getId());
        vo.setName(knowledgeBase.getName());
        vo.setDescription(knowledgeBase.getDescription());
        vo.setStatus(knowledgeBase.getStatus());
        vo.setStatusName(convertStatusName(knowledgeBase.getStatus()));
        vo.setCreatedBy(knowledgeBase.getCreatedBy());
        vo.setCreateTime(knowledgeBase.getCreateTime());
        vo.setUpdateTime(knowledgeBase.getUpdateTime());

        vo.setDocumentCount(countDocuments(knowledgeBase.getId()));
        vo.setChunkCount(countChunks(knowledgeBase.getId()));
        vo.setVectorizedChunkCount(countVectorizedChunks(knowledgeBase.getId()));

        return vo;
    }

    /**
     * 统计知识库下文档数量
     */
    private Long countDocuments(Long knowledgeBaseId) {
        return knowledgeDocumentMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
        );
    }

    /**
     * 统计知识库下切片数量
     */
    private Long countChunks(Long knowledgeBaseId) {
        return documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
        );
    }

    /**
     * 统计知识库下已向量化切片数量
     */
    private Long countVectorizedChunks(Long knowledgeBaseId) {
        return documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(DocumentChunk::getVectorized, 1)
        );
    }

    /**
     * 状态编码转中文
     */
    private String convertStatusName(String status) {
        if (KnowledgeBaseStatusEnum.ENABLED.getCode().equals(status)) {
            return KnowledgeBaseStatusEnum.ENABLED.getName();
        }

        if (KnowledgeBaseStatusEnum.DISABLED.getCode().equals(status)) {
            return KnowledgeBaseStatusEnum.DISABLED.getName();
        }

        return "未知状态";
    }
}