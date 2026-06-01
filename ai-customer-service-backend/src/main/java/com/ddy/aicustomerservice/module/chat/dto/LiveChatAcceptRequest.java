package com.ddy.aicustomerservice.module.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 客服接入会话请求（乐观锁）
 */
@Data
public class LiveChatAcceptRequest {

    /**
     * 客户端读取到的会话 version，与数据库一致方可接入
     */
    @NotNull(message = "expectedVersion 不能为空")
    private Integer expectedVersion;
}
