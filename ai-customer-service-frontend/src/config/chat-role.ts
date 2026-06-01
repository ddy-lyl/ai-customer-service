import type { RoleCode } from '@/api/types';

export interface ChatRoleConfig {
  title: string;
  subtitle: string;
  welcome: string;
  placeholder: string;
  toolsLabel: string;
  toolsHint: string;
  quickAsks: { icon: string; text: string }[];
}

const USER_CHAT: ChatRoleConfig = {
  title: '云屿售后助手',
  subtitle: '售后政策 · 查订单 · 建工单',
  welcome: '我是云屿 AI，可以解答退货退款政策、查询您的订单与工单进度，必要时帮您提交售后工单。',
  placeholder: '例如：我的订单 ORD202605010001 到哪了？',
  toolsLabel: '智能动作',
  toolsHint: '检测到订单/工单关键词时启用工具查询',
  quickAsks: [
    { icon: '📦', text: '帮我查订单 ORD202605010001' },
    { icon: '↩️', text: '七天无理由怎么退货？' },
    { icon: '🎫', text: '我的工单处理到哪了？' },
    { icon: '💬', text: '商品坏了要申请售后' }
  ]
};

const STAFF_CHAT: ChatRoleConfig = {
  title: '客服 AI 助手',
  subtitle: '工单摘要 · 推荐回复 · 订单上下文',
  welcome: '我是客服工作台 AI，可帮你查订单/工单、列出待处理工单、生成回复建议与处理方案，请结合实际情况发送给用户。',
  placeholder: '例如：查订单 ORD202605120004，并给退款话术建议',
  toolsLabel: '工作台工具',
  toolsHint: '启用后可查全平台订单、列工单、查工单状态',
  quickAsks: [
    { icon: '📋', text: '列出我待处理的工单' },
    { icon: '📦', text: '查订单 ORD202605120004 的详情' },
    { icon: '💡', text: '键盘退款工单怎么回复用户？' },
    { icon: '⚡', text: '有哪些高优先级待认领工单？' }
  ]
};

const ADMIN_CHAT: ChatRoleConfig = {
  title: '运营 AI 助手',
  subtitle: '热点分析 · 工单统计 · 知识库优化',
  welcome: '我是管理后台 AI，可分析平台工单/订单概况、用户热点问题，并给出知识库与 AI 回答质量优化建议。',
  placeholder: '例如：最近用户最常问什么？退款类工单占比多少？',
  toolsLabel: '运营分析',
  toolsHint: '启用平台概览、热点问题等管理员工具',
  quickAsks: [
    { icon: '📊', text: '给我平台工单和订单概况' },
    { icon: '🔥', text: '最近用户最常问什么问题？' },
    { icon: '📦', text: '查订单 ORD202605120004' },
    { icon: '📚', text: '知识库有哪些需要优化的地方？' }
  ]
};

export function resolveChatConfig(roles: RoleCode[]): ChatRoleConfig {
  if (roles.includes('ADMIN')) return ADMIN_CHAT;
  if (roles.includes('STAFF')) return STAFF_CHAT;
  return USER_CHAT;
}
