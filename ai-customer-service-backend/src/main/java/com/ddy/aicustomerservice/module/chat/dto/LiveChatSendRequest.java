package com.ddy.aicustomerservice.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 人工会话中发送消息
 */
@Data
public class LiveChatSendRequest {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 4000, message = "消息过长")
    private String content;
}
