package com.ddy.aicustomerservice.module.chat.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:55
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息响应对象
 */
@Data
public class ChatMessageVO {

    private Long id;

    private Long sessionId;

    private Long userId;

    private String role;

    private String roleName;

    private Long senderId;

    private String senderName;

    private String content;

    /**
     * 用户意图编码（仅 USER / ASSISTANT 消息有值）
     */
    private String intent;

    /**
     * 模型名称（仅 ASSISTANT 消息有值）
     */
    private String modelName;

    private LocalDateTime createTime;
}
