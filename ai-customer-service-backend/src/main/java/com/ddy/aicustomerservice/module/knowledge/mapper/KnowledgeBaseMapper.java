package com.ddy.aicustomerservice.module.knowledge.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:08
 **/
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.knowledge.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
}