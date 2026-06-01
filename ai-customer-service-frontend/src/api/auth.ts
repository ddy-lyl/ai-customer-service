import http from './http';
import type { LoginResult, UserInfo } from './types';

export const authApi = {
  login(payload: { username: string; password: string }) {
    return http.post<LoginResult>('/api/auth/login', payload);
  },
  register(payload: {
    username: string;
    password: string;
    confirmPassword: string;
    nickname: string;
    phone?: string;
    email?: string;
  }) {
    return http.post<null>('/api/auth/register', payload);
  },
  me() {
    return http.get<UserInfo>('/api/auth/me');
  },
  logout() {
    return http.post<null>('/api/auth/logout');
  }
};
