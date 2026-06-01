import type { RoleCode } from '@/api/types';

export interface NavItem {
  name: string;
  to: string;
  icon: string;
  roles: RoleCode[];
  badge?: string;
}

export interface NavGroup {
  title: string;
  items: NavItem[];
}

/** 侧栏导航：与 router meta.roles 保持一致 */
export const NAV_GROUPS: NavGroup[] = [
  {
    title: '工作台',
    items: [
      {
        name: 'AI 助手',
        to: '/chat',
        icon: 'sparkles',
        roles: ['USER', 'STAFF', 'ADMIN'],
        badge: 'new'
      }
    ]
  },
  {
    title: '用户服务',
    items: [
      { name: '我的订单', to: '/orders', icon: 'order', roles: ['USER'] },
      { name: '我的工单', to: '/tickets', icon: 'ticket', roles: ['USER'] }
    ]
  },
  {
    title: '客服中心',
    items: [
      { name: '在线客服', to: '/staff/live-chat', icon: 'message', roles: ['STAFF'] },
      { name: '待认领工单', to: '/staff/tickets/pool', icon: 'ticket', roles: ['STAFF'] },
      { name: '我的处理工单', to: '/staff/tickets/mine', icon: 'tool', roles: ['STAFF'] }
    ]
  },
  {
    title: '管理后台',
    items: [
      { name: '订单管理', to: '/admin/orders', icon: 'order', roles: ['ADMIN'] },
      { name: '工单管理', to: '/admin/tickets', icon: 'flow', roles: ['ADMIN'] },
      { name: '用户管理', to: '/admin/users', icon: 'users', roles: ['ADMIN'] },
      { name: '知识库管理', to: '/admin/knowledge', icon: 'book', roles: ['ADMIN'] },
      { name: 'AI 链路追踪', to: '/admin/ai-logs', icon: 'logs', roles: ['ADMIN'] }
    ]
  }
];
