import { ref } from 'vue';

export interface ToastItem {
  id: number;
  type: 'info' | 'success' | 'warning' | 'error';
  message: string;
  duration: number;
}

const toasts = ref<ToastItem[]>([]);
let _id = 1;

function push(type: ToastItem['type'], message: string, duration = 2400) {
  const id = _id++;
  toasts.value.push({ id, type, message, duration });
  if (duration > 0) {
    setTimeout(() => {
      remove(id);
    }, duration);
  }
}
function remove(id: number) {
  toasts.value = toasts.value.filter((t) => t.id !== id);
}

export function useToast() {
  return {
    toasts,
    info: (msg: string, d?: number) => push('info', msg, d),
    success: (msg: string, d?: number) => push('success', msg, d),
    warning: (msg: string, d?: number) => push('warning', msg, d),
    error: (msg: string, d?: number) => push('error', msg, d),
    remove
  };
}
