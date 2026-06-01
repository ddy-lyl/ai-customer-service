package com.ddy.aicustomerservice.module.chat.service;

/**
 * @author 罗亚兰
 * @date 2026/5/19 20:56
 **/
import com.ddy.aicustomerservice.common.model.PageQuery;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.chat.dto.KnowledgeQaRequest;
import com.ddy.aicustomerservice.module.chat.vo.ChatMessageVO;
import com.ddy.aicustomerservice.module.chat.vo.ChatSessionVO;
import com.ddy.aicustomerservice.module.chat.vo.KnowledgeQaResponseVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI知识库问答业务接口
 */
public interface AiKnowledgeChatService {

    /**
     * 知识库问答（同步）
     *
     * 支持 Function Calling，能在本轮触发查订单 / 建工单 / 查工单工具。
     */
    KnowledgeQaResponseVO ask(KnowledgeQaRequest request);

    /**
     * 知识库问答（SSE 流式）
     *
     * 用于聊天页"打字机"体验，按 token 推送给前端。
     * 当前实现不挂载业务工具（Plan B）：
     * - 涉及订单/工单等需要工具的场景请走同步 {@link #ask(KnowledgeQaRequest)}；
     * - 流式只做 RAG 问答 + 通用聊天。
     *
     * 事件协议：
     * event: meta   data: {sessionId, userMessageId, questionText, retrievedCount, sources}
     * event: delta  data: {content}        // 多次推送
     * event: done   data: {sessionId, assistantMessageId, fullAnswer, intent, modelName}
     * event: error  data: {message}        // 出错时
     */
    SseEmitter askStream(KnowledgeQaRequest request);

    /**
     * 分页查询我的会话
     */
    PageResult<ChatSessionVO> pageMySessions(PageQuery query);

    /**
     * 查询某个会话的消息列表
     */
    List<ChatMessageVO> listMessages(Long sessionId, Long afterMessageId);
}