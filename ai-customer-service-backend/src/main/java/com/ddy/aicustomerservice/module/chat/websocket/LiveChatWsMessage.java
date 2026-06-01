package com.ddy.aicustomerservice.module.chat.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 下行消息信封
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveChatWsMessage {

    private String type;

    private Object payload;
}
