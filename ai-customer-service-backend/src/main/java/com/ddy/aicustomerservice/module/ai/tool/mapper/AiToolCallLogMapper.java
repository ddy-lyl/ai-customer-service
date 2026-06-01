package com.ddy.aicustomerservice.module.ai.tool.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/19 22:49
 **/
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.ai.tool.entity.AiToolCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 工具调用日志 Mapper
 */
@Mapper
public interface AiToolCallLogMapper extends BaseMapper<AiToolCallLog> {
}