package com.ddy.aicustomerservice.module.chat.vo;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:55
 **/
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话响应对象
 */
@Data
public class ChatSessionVO {

    private Long id;

    private Long userId;

    private String title;

    private String status;

    private String statusName;

    private String serviceMode;

    private String serviceModeName;

    private Long assignedStaffId;

    private String assignedStaffName;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}