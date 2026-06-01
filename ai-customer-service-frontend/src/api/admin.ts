import http from './http';
import type {
  AiRetrievalLogVO,
  AiToolCallVO,
  AiTraceVO,
  ChatMessageVO,
  ChatSessionVO,
  KnowledgeBaseVO,
  KnowledgeDocumentVO,
  PageQuery,
  PageResult,
  UserInfo
} from './types';

/* —— 用户管理 —— */
export const adminUserApi = {
  page(params: PageQuery) {
    return http.get<PageResult<UserInfo>>('/api/admin/users/page', { params });
  },
  createStaff(payload: {
    username: string;
    password: string;
    nickname: string;
    phone?: string;
    email?: string;
  }) {
    return http.post<UserInfo>('/api/admin/users/staff', payload);
  },
  updateStatus(id: number, payload: { status: 'ENABLED' | 'DISABLED' }) {
    return http.put<null>(`/api/admin/users/${id}/status`, payload);
  },
  resetPassword(id: number, payload: { newPassword: string }) {
    return http.put<null>(`/api/admin/users/${id}/password`, payload);
  },
  staffList() {
    return http.get<UserInfo[]>('/api/admin/users/staff/list');
  }
};

/* —— 知识库 —— */
export const knowledgeApi = {
  basePage(params: PageQuery) {
    return http.get<PageResult<KnowledgeBaseVO>>(
      '/api/admin/knowledge-bases/page',
      { params }
    );
  },
  createBase(payload: { name: string; description?: string }) {
    return http.post<KnowledgeBaseVO>('/api/admin/knowledge-bases', payload);
  },
  updateBase(id: number, payload: { name: string; description?: string }) {
    return http.put<null>(`/api/admin/knowledge-bases/${id}`, payload);
  },
  toggleStatus(id: number, payload: { status: 'ENABLED' | 'DISABLED' }) {
    return http.put<null>(`/api/admin/knowledge-bases/${id}/status`, payload);
  },
  deleteBase(id: number) {
    return http.delete<null>(`/api/admin/knowledge-bases/${id}`);
  },

  documentPage(params: PageQuery) {
    return http.get<PageResult<KnowledgeDocumentVO>>(
      '/api/admin/knowledge-documents/page',
      { params }
    );
  },
  upload(knowledgeBaseId: number, file: File) {
    const form = new FormData();
    form.append('knowledgeBaseId', String(knowledgeBaseId));
    form.append('file', file);
    return http.post<KnowledgeDocumentVO>(
      '/api/admin/knowledge-documents/upload',
      form,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },
  parseOne(id: number) {
    return http.post<null>(`/api/admin/knowledge-documents/${id}/parse`);
  },
  parseAll(knowledgeBaseId: number) {
    return http.post<null>(
      `/api/admin/knowledge-documents/parse-all?knowledgeBaseId=${knowledgeBaseId}`
    );
  },
  chunk(documentId: number) {
    return http.post<null>(`/api/admin/document-chunks/${documentId}/chunk`);
  },
  vectorize(documentId: number) {
    return http.post<null>(
      `/api/admin/document-vectors/document/${documentId}/vectorize`
    );
  },
  deleteDocument(id: number) {
    return http.delete<null>(`/api/admin/knowledge-documents/${id}`);
  }
};

/* —— AI 日志 —— */
export const aiLogsApi = {
  sessionsPage(params: PageQuery) {
    return http.get<PageResult<ChatSessionVO>>(
      '/api/admin/ai-logs/sessions/page',
      { params }
    );
  },
  sessionMessages(sessionId: number) {
    return http.get<ChatMessageVO[]>(
      `/api/admin/ai-logs/sessions/${sessionId}/messages`
    );
  },
  trace(sessionId: number) {
    return http.get<AiTraceVO>(`/api/admin/ai-logs/sessions/${sessionId}/trace`);
  },
  retrievalPage(params: PageQuery) {
    return http.get<PageResult<AiRetrievalLogVO>>(
      '/api/admin/ai-logs/retrieval/page',
      { params }
    );
  },
  toolsPage(params: PageQuery) {
    return http.get<PageResult<AiToolCallVO>>('/api/admin/ai-logs/tools/page', {
      params
    });
  }
};
