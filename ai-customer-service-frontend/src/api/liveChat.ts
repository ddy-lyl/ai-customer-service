import http from './http';
import type {
  ChatMessageVO,
  ChatSessionDetailVO,
  HandoffResult,
  LiveChatSessionVO,
  PageQuery,
  PageResult
} from './types';

export interface LiveSendPayload {
  content: string;
}

export const liveChatApi = {
  sessionDetail(sessionId: number) {
    return http.get<ChatSessionDetailVO>(
      `/api/ai/knowledge-chat/sessions/${sessionId}`
    );
  },

  requestHandoff(sessionId: number) {
    return http.post<HandoffResult>(
      `/api/ai/knowledge-chat/sessions/${sessionId}/handoff`
    );
  },

  sendUserMessage(sessionId: number, payload: LiveSendPayload) {
    return http.post<ChatMessageVO>(
      `/api/ai/knowledge-chat/sessions/${sessionId}/messages`,
      payload
    );
  },

  pollMessages(sessionId: number, afterMessageId?: number | null) {
    return http.get<ChatMessageVO[]>(
      `/api/ai/knowledge-chat/sessions/${sessionId}/messages`,
      { params: afterMessageId ? { afterMessageId } : undefined }
    );
  },

  staffWaiting(params?: PageQuery) {
    return http.get<PageResult<LiveChatSessionVO>>(
      '/api/staff/live-chat/sessions/waiting',
      { params }
    );
  },

  staffMine(params?: PageQuery) {
    return http.get<PageResult<LiveChatSessionVO>>(
      '/api/staff/live-chat/sessions/mine',
      { params }
    );
  },

  staffSessionDetail(sessionId: number) {
    return http.get<ChatSessionDetailVO>(
      `/api/staff/live-chat/sessions/${sessionId}`
    );
  },

  staffAccept(sessionId: number, expectedVersion: number) {
    return http.post<void>(`/api/staff/live-chat/sessions/${sessionId}/accept`, {
      expectedVersion
    });
  },

  staffEnd(sessionId: number) {
    return http.post<void>(`/api/staff/live-chat/sessions/${sessionId}/end`);
  },

  staffSend(sessionId: number, payload: LiveSendPayload) {
    return http.post<ChatMessageVO>(
      `/api/staff/live-chat/sessions/${sessionId}/messages`,
      payload
    );
  },

  staffPollMessages(sessionId: number, afterMessageId?: number | null) {
    return http.get<ChatMessageVO[]>(
      `/api/staff/live-chat/sessions/${sessionId}/messages`,
      { params: afterMessageId ? { afterMessageId } : undefined }
    );
  }
};
