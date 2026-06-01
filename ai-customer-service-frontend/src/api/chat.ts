import http from './http';
import { buildAuthorizationHeader } from './auth-token';
import type {
  AskResult,
  ChatMessageVO,
  ChatSessionVO,
  PageQuery,
  PageResult
} from './types';

export interface AskPayload {
  sessionId?: number | null;
  questionText: string;
  knowledgeBaseId?: number | null;
  topK?: number;
  similarityThreshold?: number;
}

export const chatApi = {
  ask(payload: AskPayload) {
    return http.post<AskResult>('/api/ai/knowledge-chat/ask', payload);
  },
  mySessions(params?: PageQuery) {
    return http.get<PageResult<ChatSessionVO>>('/api/ai/knowledge-chat/sessions/my', {
      params
    });
  },
  sessionMessages(sessionId: number, afterMessageId?: number | null) {
    return http.get<ChatMessageVO[]>(
      `/api/ai/knowledge-chat/sessions/${sessionId}/messages`,
      { params: afterMessageId ? { afterMessageId } : undefined }
    );
  }
};

/* ============================================================================
 * SSE 流式：fetch + ReadableStream
 * 后端需要 Authorization 头，因此不用原生 EventSource。
 * ============================================================================ */
export interface StreamCallbacks {
  onMeta?: (data: import('./types').StreamMetaPayload) => void;
  onDelta?: (token: string) => void;
  onDone?: (data: import('./types').StreamDonePayload) => void;
  onError?: (data: import('./types').StreamErrorPayload) => void;
}

export async function askStream(
  payload: AskPayload,
  callbacks: StreamCallbacks,
  signal?: AbortSignal
): Promise<void> {
  const authorization = buildAuthorizationHeader();
  const base = (import.meta.env.VITE_API_BASE as string | undefined) ?? '';
  const resp = await fetch(`${base}/api/ai/knowledge-chat/ask/stream`, {
    method: 'POST',
    signal,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(authorization ? { Authorization: authorization } : {})
    },
    body: JSON.stringify(payload)
  });

  if (!resp.ok || !resp.body) {
    callbacks.onError?.({ message: `请求失败：HTTP ${resp.status}` });
    return;
  }

  const reader = resp.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  const dispatchSseBlock = (raw: string) => {
    if (!raw.trim()) return;
    let event = 'message';
    const dataLines: string[] = [];
    raw.split('\n').forEach((line) => {
      if (line.startsWith('event:')) event = line.slice(6).trim();
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim());
    });
    if (!dataLines.length) return;
    let data: unknown;
    try {
      data = JSON.parse(dataLines.join('\n'));
    } catch {
      return;
    }

    if (event === 'meta') callbacks.onMeta?.(data as never);
    else if (event === 'delta') callbacks.onDelta?.((data as { content: string }).content);
    else if (event === 'done') callbacks.onDone?.(data as never);
    else if (event === 'error') callbacks.onError?.(data as never);
  };

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        buffer += decoder.decode();
        dispatchSseBlock(buffer);
        break;
      }
      buffer += decoder.decode(value, { stream: true });

      let idx: number;
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const raw = buffer.slice(0, idx);
        buffer = buffer.slice(idx + 2);
        dispatchSseBlock(raw);
      }
    }
  } catch (err) {
    if ((err as Error).name === 'AbortError') return;
    callbacks.onError?.({ message: (err as Error).message || '流式连接中断' });
  }
}
