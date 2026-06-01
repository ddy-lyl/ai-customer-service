/**
 * 从 localStorage 读取 JWT，并规范化格式（去空格、去掉重复的 Bearer 前缀）。
 */
export function getStoredToken(): string | null {
  const raw = localStorage.getItem('token')?.trim();
  if (!raw) return null;
  if (raw.toLowerCase().startsWith('bearer ')) {
    return raw.slice(7).trim() || null;
  }
  return raw;
}

export function buildAuthorizationHeader(): string | null {
  const token = getStoredToken();
  return token ? `Bearer ${token}` : null;
}
