package com.ddy.aicustomerservice.module.admin.ailog.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:08
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台AI会话响应对象
 */
@Data
public class AiChatSessionAdminVO {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String title;

    private String status;

    private String statusName;

    private LocalDateTime lastMessageTime;

    /**
     * 消息数量
     */
    private Long messageCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}