import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { resolveFallbackPath, resolveHomePath } from '@/config/role-home';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => {
      const user = useUserStore();
      return user.token ? resolveHomePath(user.roles) : '/login';
    }
  },

  {
    path: '/login',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      { path: '', name: 'login', component: () => import('@/views/auth/LoginView.vue') }
    ],
    meta: { public: true }
  },
  {
    path: '/register',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      { path: '', name: 'register', component: () => import('@/views/auth/RegisterView.vue') }
    ],
    meta: { public: true }
  },

  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'chat',
        name: 'chat',
        component: () => import('@/views/chat/ChatView.vue'),
        meta: { title: 'AI 助手', roles: ['USER', 'STAFF', 'ADMIN'] }
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/user/OrdersView.vue'),
        meta: { title: '我的订单', roles: ['USER'] }
      },
      {
        path: 'tickets',
        name: 'tickets',
        component: () => import('@/views/user/TicketsView.vue'),
        meta: { title: '我的工单', roles: ['USER'] }
      },
      {
        path: 'tickets/:id',
        name: 'ticket-detail',
        component: () => import('@/views/user/TicketDetailView.vue'),
        meta: { title: '工单详情', roles: ['USER', 'STAFF', 'ADMIN'] }
      },

      {
        path: 'staff/live-chat',
        name: 'staff-live-chat',
        component: () => import('@/views/staff/StaffLiveChatView.vue'),
        meta: { title: '在线客服', roles: ['STAFF'] }
      },
      {
        path: 'staff/tickets',
        redirect: '/staff/tickets/pool'
      },
      {
        path: 'staff/tickets/pool',
        name: 'staff-tickets-pool',
        component: () => import('@/views/staff/StaffTicketsView.vue'),
        meta: { title: '待认领工单', roles: ['STAFF'], scopeMode: 'POOL' }
      },
      {
        path: 'staff/tickets/mine',
        name: 'staff-tickets-mine',
        component: () => import('@/views/staff/StaffTicketsView.vue'),
        meta: { title: '我的处理工单', roles: ['STAFF'], scopeMode: 'MINE' }
      },

      {
        path: 'admin/orders',
        name: 'admin-orders',
        component: () => import('@/views/admin/AdminOrdersView.vue'),
        meta: { title: '订单管理', roles: ['ADMIN'] }
      },
      {
        path: 'admin/users',
        name: 'admin-users',
        component: () => import('@/views/admin/UsersView.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'admin/tickets',
        name: 'admin-tickets',
        component: () => import('@/views/admin/AdminTicketsView.vue'),
        meta: { title: '工单管理', roles: ['ADMIN'] }
      },
      {
        path: 'admin/knowledge',
        name: 'admin-knowledge',
        component: () => import('@/views/admin/KnowledgeView.vue'),
        meta: { title: '知识库管理', roles: ['ADMIN'] }
      },
      {
        path: 'admin/ai-logs',
        name: 'admin-ai-logs',
        component: () => import('@/views/admin/AiLogsView.vue'),
        meta: { title: 'AI 链路追踪', roles: ['ADMIN'] }
      }
    ]
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { public: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
});

router.beforeEach((to) => {
  const user = useUserStore();
  if (to.meta.public) return true;
  if (to.meta.requiresAuth && !user.token) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  const allow = to.meta.roles as string[] | undefined;
  if (allow && allow.length && !allow.some((r) => user.roles.includes(r as 'USER'))) {
    return { path: resolveFallbackPath(user.roles) };
  }
  return true;
});

export default router;
