import type { ChatMessageVO, ChatSessionDetailVO } from '@/api/types';

export type LiveChatWsEventType = 'MESSAGE' | 'SESSION_UPDATE' | 'ERROR';

export interface LiveChatWsEnvelope {
  type: LiveChatWsEventType;
  payload: ChatMessageVO | ChatSessionDetailVO | { message?: string };
}

function wsBaseUrl(): string {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host;
  return `${proto}//${host}`;
}

export type LiveChatWsHandlers = {
  onMessage?: (msg: ChatMessageVO) => void;
  onSessionUpdate?: (detail: ChatSessionDetailVO) => void;
  onError?: (message: string) => void;
  onClose?: () => void;
};

/**
 * 订阅在线客服会话 WebSocket 推送（替代 3s 轮询）
 */
export function connectLiveChatWs(
  sessionId: number,
  token: string,
  handlers: LiveChatWsHandlers
): WebSocket | null {
  if (!token?.trim()) {
    return null;
  }
  const url = `${wsBaseUrl()}/ws/live-chat?token=${encodeURIComponent(token.trim())}`;
  const ws = new WebSocket(url);

  ws.onopen = () => {
    ws.send(JSON.stringify({ action: 'subscribe', sessionId }));
  };

  ws.onmessage = (ev) => {
    try {
      const data = JSON.parse(ev.data as string) as LiveChatWsEnvelope;
      if (data.type === 'MESSAGE' && handlers.onMessage) {
        handlers.onMessage(data.payload as ChatMessageVO);
      } else if (data.type === 'SESSION_UPDATE' && handlers.onSessionUpdate) {
        handlers.onSessionUpdate(data.payload as ChatSessionDetailVO);
      } else if (data.type === 'ERROR' && handlers.onError) {
        const p = data.payload as { message?: string };
        handlers.onError(p?.message ?? '连接异常');
      }
    } catch {
      // ignore malformed frames
    }
  };

  ws.onclose = () => {
    handlers.onClose?.();
  };

  ws.onerror = () => {
    handlers.onError?.('WebSocket 连接失败');
  };

  return ws;
}
