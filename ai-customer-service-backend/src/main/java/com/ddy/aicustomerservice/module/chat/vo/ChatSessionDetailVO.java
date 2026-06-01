package com.ddy.aicustomerservice.module.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话详情（含人工接待状态）
 */
@Data
public class ChatSessionDetailVO {

    private Long id;

    private Long userId;

    private String title;

    private String status;

    private String statusName;

    private String serviceMode;

    private String serviceModeName;

    private Long assignedStaffId;

    private String assignedStaffName;

    private Long activeTicketId;

    private String activeTicketNo;

    private String handoffReason;

    private String handoffReasonName;

    private LocalDateTime handoffTime;

    private LocalDateTime lastMessageTime;

    /**
     * 乐观锁版本（接入会话时需携带 expectedVersion）
     */
    private Integer version;
}
