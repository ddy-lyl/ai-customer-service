package com.ddy.aicustomerservice.module.document.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/18 21:09
 **/
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.document.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档切片 Mapper
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
}