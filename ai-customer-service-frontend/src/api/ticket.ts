import http from './http';
import type {
  PageQuery,
  PageResult,
  TicketFlowVO,
  TicketPriority,
  TicketType,
  TicketVO
} from './types';

export interface CreateTicketPayload {
  orderNo?: string | null;
  title: string;
  type: TicketType;
  description: string;
  priority: TicketPriority;
  onBehalfUserId?: number;
}

export const ticketApi = {
  create(payload: CreateTicketPayload) {
    return http.post<TicketVO>('/api/tickets', payload);
  },
  myPage(params: PageQuery) {
    return http.get<PageResult<TicketVO>>('/api/tickets/my', { params });
  },
  detail(id: number) {
    return http.get<TicketVO>(`/api/tickets/${id}`);
  },
  flows(id: number) {
    return http.get<TicketFlowVO[]>(`/api/tickets/${id}/flows`);
  },
  close(id: number, payload: { closeReason: string }) {
    return http.put<null>(`/api/tickets/${id}/close`, payload);
  },
  cancel(id: number, payload?: { cancelReason?: string }) {
    return http.put<null>(`/api/tickets/${id}/cancel`, payload ?? {});
  },

  /* 客服 */
  staffCreateOnBehalf(payload: CreateTicketPayload & { onBehalfUserId: number }) {
    return http.post<TicketVO>('/api/staff/tickets', payload);
  },
  staffPage(params: PageQuery & { scope: 'POOL' | 'MINE' }) {
    return http.get<PageResult<TicketVO>>('/api/staff/tickets/page', { params });
  },
  staffClaim(id: number) {
    return http.put<null>(`/api/staff/tickets/${id}/claim`);
  },
  staffProcess(
    id: number,
    payload: { processResult: string; remark?: string; resolved: boolean }
  ) {
    return http.put<null>(`/api/staff/tickets/${id}/process`, payload);
  },

  /* 管理员 */
  adminPage(params: PageQuery) {
    return http.get<PageResult<TicketVO>>('/api/admin/tickets/page', { params });
  },
  adminAssign(id: number, payload: { staffId: number; remark?: string }) {
    return http.put<null>(`/api/admin/tickets/${id}/assign`, payload);
  }
};
