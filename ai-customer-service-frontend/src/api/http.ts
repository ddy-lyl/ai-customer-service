/* ============================================================================
 * Axios 封装：统一鉴权 / 错误处理 / 响应解包
 * ============================================================================ */
import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios';
import type { ApiResult } from './types';
import { useToast } from '@/composables/useToast';
import { useUserStore } from '@/stores/user';
import { buildAuthorizationHeader } from './auth-token';

const BASE_URL = (import.meta.env.VITE_API_BASE as string | undefined) ?? '';

const instance: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 60_000
});

/** 防止多个并发 401 连续弹两次「登录已过期」 */
let handlingUnauthorized = false;

function handleUnauthorized() {
  if (handlingUnauthorized) return;
  handlingUnauthorized = true;

  const toast = useToast();
  toast.error('登录已过期，请重新登录');

  localStorage.removeItem('token');
  localStorage.removeItem('userInfo');

  try {
    const user = useUserStore();
    user.$patch({
      token: '',
      userId: null,
      username: '',
      nickname: '',
      roles: [],
      profile: null
    });
  } catch {
    /* Pinia 未初始化时忽略 */
  }

  window.dispatchEvent(new CustomEvent('app:unauthorized'));

  window.setTimeout(() => {
    handlingUnauthorized = false;
  }, 1500);
}

instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const authorization = buildAuthorizationHeader();
  if (authorization) {
    config.headers.set('Authorization', authorization);
  }
  return config;
});

instance.interceptors.response.use(
  (resp) => {
    const result = resp.data as ApiResult;
    if (!result || typeof result.code === 'undefined') return resp.data;
    if (result.code === 200) return result.data;

    const toast = useToast();
    if (result.code === 401) {
      handleUnauthorized();
    } else {
      toast.error(result.message || '请求失败');
    }
    return Promise.reject(result);
  },
  (err) => {
    const toast = useToast();
    const status = err?.response?.status;
    const bodyCode = err?.response?.data?.code;

    if (status === 401 || bodyCode === 401) {
      handleUnauthorized();
    } else {
      toast.error(err?.response?.data?.message || err.message || '网络异常');
    }
    return Promise.reject(err);
  }
);

export type Resp<T> = Promise<T>;

export const http = {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Resp<T> {
    return instance.get(url, config) as unknown as Resp<T>;
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Resp<T> {
    return instance.post(url, data, config) as unknown as Resp<T>;
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Resp<T> {
    return instance.put(url, data, config) as unknown as Resp<T>;
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Resp<T> {
    return instance.delete(url, config) as unknown as Resp<T>;
  }
};

export default http;
