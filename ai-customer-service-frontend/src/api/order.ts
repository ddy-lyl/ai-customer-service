import http from './http';
import type { OrderVO, PageQuery, PageResult } from './types';

export const orderApi = {
  my() {
    return http.get<OrderVO[]>('/api/orders/my');
  },
  detail(orderNo: string) {
    return http.get<OrderVO>(`/api/orders/${orderNo}`);
  },
  staffDetail(orderNo: string) {
    return http.get<OrderVO>(`/api/staff/orders/${orderNo}`);
  },
  adminPage(params: PageQuery & { orderNo?: string; productName?: string; status?: string; userId?: number }) {
    return http.get<PageResult<OrderVO>>('/api/admin/orders/page', { params });
  },
  adminDetail(orderNo: string) {
    return http.get<OrderVO>(`/api/admin/orders/${orderNo}`);
  }
};
