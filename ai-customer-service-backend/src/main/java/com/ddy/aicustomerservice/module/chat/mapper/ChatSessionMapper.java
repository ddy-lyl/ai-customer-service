package com.ddy.aicustomerservice.module.chat.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:52
 **/
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}