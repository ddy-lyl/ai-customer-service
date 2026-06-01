package com.ddy.aicustomerservice.module.chat.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:53
 **/
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}