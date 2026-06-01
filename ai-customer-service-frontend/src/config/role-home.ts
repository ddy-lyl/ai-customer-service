import type { RoleCode } from '@/api/types';

/** 登录后 / 无权限时的默认首页 */
export function resolveHomePath(roles: RoleCode[]): string {
  if (roles.includes('ADMIN')) return '/admin/tickets';
  if (roles.includes('STAFF')) return '/staff/tickets/pool';
  return '/chat';
}

/** 无权限访问某路由时的回退页 */
export function resolveFallbackPath(roles: RoleCode[]): string {
  return resolveHomePath(roles);
}
