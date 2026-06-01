import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

export function formatTime(value?: string | null, fmt = 'YYYY-MM-DD HH:mm') {
  if (!value) return '—';
  return dayjs(value).format(fmt);
}

export function fromNow(value?: string | null) {
  if (!value) return '';
  return dayjs(value).fromNow();
}

export function formatAmount(value: number | null | undefined) {
  if (value == null) return '—';
  return `¥${Number(value).toFixed(2)}`;
}

export function formatFileSize(bytes: number | null | undefined) {
  if (!bytes && bytes !== 0) return '—';
  const u = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let v = bytes;
  while (v >= 1024 && i < u.length - 1) {
    v /= 1024;
    i++;
  }
  return `${v.toFixed(v < 10 && i > 0 ? 1 : 0)} ${u[i]}`;
}

export function shorten(text: string | null | undefined, max = 30) {
  if (!text) return '';
  return text.length > max ? text.slice(0, max) + '…' : text;
}
