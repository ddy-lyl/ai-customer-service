/* 枚举映射 + 标签色调，集中维护中文名 / chip 配色 */
import type {
  OrderStatus,
  TicketPriority,
  TicketSource,
  TicketStatus,
  TicketType
} from '@/api/types';

export type ChipTone =
  | 'primary'
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'mint'
  | 'iris'
  | 'peach';

interface EnumMeta {
  label: string;
  tone: ChipTone;
}

export const ORDER_STATUS_MAP: Record<OrderStatus, EnumMeta> = {
  WAIT_PAY:      { label: '待支付',     tone: 'warning' },
  WAIT_SHIPPING: { label: '待发货',     tone: 'info' },
  SHIPPED:       { label: '已发货',     tone: 'mint' },
  RECEIVED:      { label: '已收货',     tone: 'success' },
  REFUNDING:     { label: '退款中',     tone: 'peach' },
  REFUNDED:      { label: '已退款',     tone: 'danger' },
  CLOSED:        { label: '已关闭',     tone: 'info' }
};

export const TICKET_STATUS_MAP: Record<TicketStatus, EnumMeta> = {
  PENDING:    { label: '待处理',  tone: 'warning' },
  PROCESSING: { label: '处理中',  tone: 'primary' },
  RESOLVED:   { label: '已解决',  tone: 'success' },
  CLOSED:     { label: '已关闭',  tone: 'info' },
  CANCELLED:  { label: '已取消',  tone: 'danger' }
};

export const TICKET_TYPE_MAP: Record<TicketType, EnumMeta> = {
  REFUND:          { label: '退款',     tone: 'danger' },
  RETURN_GOODS:    { label: '退货',     tone: 'peach' },
  EXCHANGE:        { label: '换货',     tone: 'info' },
  LOGISTICS:       { label: '物流',     tone: 'mint' },
  INVOICE:         { label: '发票',     tone: 'iris' },
  PRODUCT_QUALITY: { label: '产品质量', tone: 'warning' },
  ACCOUNT:         { label: '账号',     tone: 'primary' },
  CONSULTATION:    { label: '在线转人工', tone: 'mint' },
  OTHER:           { label: '其它',     tone: 'info' }
};

export const TICKET_PRIORITY_MAP: Record<TicketPriority, EnumMeta> = {
  LOW:    { label: '低',   tone: 'info' },
  MEDIUM: { label: '中',   tone: 'primary' },
  HIGH:   { label: '高',   tone: 'warning' },
  URGENT: { label: '紧急', tone: 'danger' }
};

export const TICKET_SOURCE_MAP: Record<TicketSource, EnumMeta> = {
  USER:  { label: '用户',     tone: 'primary' },
  AI:    { label: 'AI 创建',  tone: 'iris' },
  STAFF: { label: '客服代建', tone: 'mint' }
};

export const TICKET_TYPE_OPTIONS = Object.entries(TICKET_TYPE_MAP).map(
  ([value, meta]) => ({ value: value as TicketType, label: meta.label })
);
export const TICKET_PRIORITY_OPTIONS = Object.entries(TICKET_PRIORITY_MAP).map(
  ([value, meta]) => ({ value: value as TicketPriority, label: meta.label })
);
export const TICKET_STATUS_OPTIONS = Object.entries(TICKET_STATUS_MAP).map(
  ([value, meta]) => ({ value: value as TicketStatus, label: meta.label })
);
