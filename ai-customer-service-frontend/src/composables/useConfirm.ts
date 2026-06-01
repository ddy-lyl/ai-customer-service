import { ref } from 'vue';

export interface ConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  tone?: 'primary' | 'danger';
}

interface InternalState extends ConfirmOptions {
  visible: boolean;
  resolve?: (ok: boolean) => void;
}

const state = ref<InternalState>({
  visible: false,
  message: ''
});

export function useConfirm() {
  function open(opts: ConfirmOptions) {
    return new Promise<boolean>((resolve) => {
      state.value = {
        visible: true,
        title: opts.title ?? '请确认',
        message: opts.message,
        confirmText: opts.confirmText ?? '确定',
        cancelText: opts.cancelText ?? '取消',
        tone: opts.tone ?? 'primary',
        resolve
      };
    });
  }
  function close(ok: boolean) {
    state.value.resolve?.(ok);
    state.value.visible = false;
  }
  return { state, open, close };
}
