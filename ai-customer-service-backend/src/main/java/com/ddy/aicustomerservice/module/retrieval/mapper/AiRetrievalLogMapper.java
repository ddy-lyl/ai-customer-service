package com.ddy.aicustomerservice.module.retrieval.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/19 14:23
 **/
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.retrieval.entity.AiRetrievalLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI知识召回日志 Mapper
 */
@Mapper
public interface AiRetrievalLogMapper extends BaseMapper<AiRetrievalLog> {
}
