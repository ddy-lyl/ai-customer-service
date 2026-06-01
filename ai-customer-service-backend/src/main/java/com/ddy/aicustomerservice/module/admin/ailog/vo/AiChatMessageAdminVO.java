package com.ddy.aicustomerservice.module.admin.ailog.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/20 14:08
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台AI消息响应对象
 */
@Data
public class AiChatMessageAdminVO {

    private Long id;

    private Long sessionId;

    private Long userId;

    private String username;

    private String nickname;

    /**
     * USER / ASSISTANT / SYSTEM
     */
    private String role;

    private String roleName;

    private String content;

    private String contentPreview;

    private LocalDateTime createTime;
}
