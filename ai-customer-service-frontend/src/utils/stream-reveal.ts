/**
 * 将完整文本以打字机方式逐步展示（用于同步接口整段返回时的流式观感）
 */
export interface RevealTextOptions {
  /** 每次追加的字符数 */
  chunkSize?: number;
  /** 每步间隔（毫秒） */
  delayMs?: number;
  signal?: AbortSignal;
  /** 每步回调（如滚动到底部） */
  onStep?: () => void;
}

export async function revealTextProgressively(
  setText: (value: string) => void,
  fullText: string,
  options?: RevealTextOptions
): Promise<void> {
  const text = fullText ?? '';
  if (!text.length) {
    setText('');
    return;
  }

  const chunkSize = options?.chunkSize ?? 3;
  const delayMs = options?.delayMs ?? 12;
  setText('');

  for (let i = 0; i < text.length; ) {
    if (options?.signal?.aborted) {
      setText(text);
      return;
    }
    i = Math.min(i + chunkSize, text.length);
    setText(text.slice(0, i));
    options?.onStep?.();
    if (i < text.length) {
      await sleep(delayMs);
    }
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
