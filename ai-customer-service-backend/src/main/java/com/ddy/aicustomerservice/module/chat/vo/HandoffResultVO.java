package com.ddy.aicustomerservice.module.chat.vo;

import lombok.Data;

/**
 * 转人工结果
 */
@Data
public class HandoffResultVO {

    private Boolean handoffTriggered;

    private String handoffReason;

    private String handoffReasonName;

    private String serviceMode;

    private String serviceModeName;

    private Long ticketId;

    private String ticketNo;

    private Long systemMessageId;

    private String notice;
}
