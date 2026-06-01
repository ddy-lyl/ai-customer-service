package com.ddy.aicustomerservice.module.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服工作台：待接入 / 接待中会话摘要
 */
@Data
public class LiveChatSessionVO {

    private Long sessionId;

    private Long userId;

    private String userNickname;

    private String title;

    private String serviceMode;

    private String serviceModeName;

    private Long assignedStaffId;

    private String lastUserMessage;

    private LocalDateTime lastMessageTime;

    private LocalDateTime handoffTime;

    private Long activeTicketId;

    private String activeTicketNo;

    private Integer version;
}
