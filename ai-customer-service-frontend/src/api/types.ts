/* ============================================================================
 * 后端通用响应 / 业务类型定义
 * ============================================================================ */

export interface ApiResult<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface PageQuery {
  pageNo?: number;
  pageSize?: number;
  [key: string]: unknown;
}

export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  pages: number;
  records: T[];
}

/* —— 鉴权 —— */
export type RoleCode = 'USER' | 'STAFF' | 'ADMIN';

export interface LoginResult {
  token: string;
  tokenType: string;
  userId: number;
  username: string;
  nickname: string;
  roles: RoleCode[];
}

export interface UserInfo {
  userId: number;
  username: string;
  nickname: string;
  phone: string;
  email: string;
  status: 'ENABLED' | 'DISABLED';
  roles: RoleCode[];
}

/* —— 订单 —— */
export type OrderStatus =
  | 'WAIT_PAY'
  | 'WAIT_SHIPPING'
  | 'SHIPPED'
  | 'RECEIVED'
  | 'REFUNDING'
  | 'REFUNDED'
  | 'CLOSED';

export interface OrderVO {
  id: number;
  orderNo: string;
  userId: number;
  productName: string;
  productSku: string;
  amount: number;
  status: OrderStatus;
  statusName: string;
  payTime: string | null;
  shippingTime: string | null;
  receivedTime: string | null;
  receiverName: string;
  receiverPhone: string;
  receiverAddress: string;
  remark: string;
  createTime: string;
  updateTime: string;
}

/* —— 工单 —— */
export type TicketStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'CLOSED' | 'CANCELLED';
export type TicketType =
  | 'REFUND'
  | 'RETURN_GOODS'
  | 'EXCHANGE'
  | 'LOGISTICS'
  | 'INVOICE'
  | 'PRODUCT_QUALITY'
  | 'ACCOUNT'
  | 'CONSULTATION'
  | 'OTHER';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TicketSource = 'USER' | 'AI' | 'STAFF';
export type TicketAction = 'CREATE' | 'ASSIGN' | 'CLAIM' | 'PROCESS' | 'RESOLVE' | 'CLOSE' | 'CANCEL';

export interface TicketFlowVO {
  id: number;
  ticketId: number;
  action: TicketAction;
  actionName: string;
  operatorId: number;
  operatorName: string;
  remark?: string;
  createTime: string;
}

export interface TicketVO {
  id: number;
  ticketNo: string;
  userId: number;
  userNickname: string;
  orderNo: string | null;
  sessionId: number | null;
  title: string;
  type: TicketType;
  typeName: string;
  description: string;
  status: TicketStatus;
  statusName: string;
  priority: TicketPriority;
  priorityName: string;
  staffId: number | null;
  staffNickname: string | null;
  source: TicketSource;
  sourceName: string;
  processResult: string | null;
  closeReason: string | null;
  resolvedTime: string | null;
  closedTime: string | null;
  createTime: string;
  updateTime: string;
  flows?: TicketFlowVO[];
}

/* —— 聊天 —— */
export type MessageRole = 'USER' | 'ASSISTANT' | 'STAFF' | 'SYSTEM' | 'TOOL';

export type ChatServiceMode = 'AI' | 'WAITING_AGENT' | 'HUMAN';
export type IntentType =
  | 'KNOWLEDGE_QA'
  | 'ORDER_QUERY'
  | 'TICKET_CREATE'
  | 'TICKET_STATUS_QUERY'
  | 'GENERAL_CHAT'
  | 'UNKNOWN';

export interface ChatSessionVO {
  id: number;
  userId: number;
  title: string;
  lastMessage?: string;
  serviceMode?: ChatServiceMode;
  serviceModeName?: string;
  assignedStaffId?: number | null;
  assignedStaffName?: string | null;
  createTime: string;
  updateTime: string;
}

export interface ChatSessionDetailVO {
  id: number;
  userId: number;
  title: string;
  status: string;
  statusName: string;
  serviceMode: ChatServiceMode;
  serviceModeName: string;
  assignedStaffId?: number | null;
  assignedStaffName?: string | null;
  activeTicketId?: number | null;
  activeTicketNo?: string | null;
  handoffReason?: string | null;
  handoffReasonName?: string | null;
  handoffTime?: string | null;
  lastMessageTime?: string | null;
  version?: number;
}

export interface LiveChatSessionVO {
  sessionId: number;
  userId: number;
  userNickname?: string;
  title: string;
  serviceMode: ChatServiceMode;
  serviceModeName: string;
  assignedStaffId?: number | null;
  lastUserMessage?: string;
  lastMessageTime?: string;
  handoffTime?: string;
  activeTicketId?: number | null;
  activeTicketNo?: string | null;
  version?: number;
}

export interface HandoffResult {
  handoffTriggered: boolean;
  handoffReason?: string;
  handoffReasonName?: string;
  serviceMode?: ChatServiceMode;
  serviceModeName?: string;
  ticketId?: number;
  ticketNo?: string;
  systemMessageId?: number;
  notice?: string;
}

export interface ChatMessageVO {
  id: number;
  sessionId: number;
  userId: number;
  role: MessageRole;
  roleName: string;
  senderId?: number | null;
  senderName?: string | null;
  content: string;
  intent?: IntentType | null;
  modelName?: string | null;
  createTime: string;
}

export interface RetrievalSource {
  rankNo?: number;
  chunkId?: number;
  documentId?: number;
  documentName?: string;
  knowledgeBaseId?: number;
  knowledgeBaseName?: string;
  score?: number;
  chunkContentPreview: string;
}

export interface AskResult {
  sessionId: number;
  userMessageId: number;
  assistantMessageId: number;
  questionText: string;
  answer: string;
  retrievedCount: number;
  sources: RetrievalSource[];
  intent?: IntentType;
  modelName?: string;
  handoffTriggered?: boolean;
  serviceMode?: ChatServiceMode;
  handoffNotice?: string;
  handoffTicketId?: number;
  handoffTicketNo?: string;
}

export interface StreamMetaPayload {
  sessionId: number;
  userMessageId: number;
  questionText: string;
  retrievedCount: number;
  sources: RetrievalSource[];
}
export interface StreamDeltaPayload { content: string; }
export interface StreamDonePayload {
  sessionId: number;
  handoffTriggered?: boolean;
  serviceMode?: ChatServiceMode;
  handoffNotice?: string;
  handoffTicketId?: number;
  handoffTicketNo?: string;
  assistantMessageId: number;
  fullAnswer: string;
  intent?: IntentType;
  modelName?: string;
}
export interface StreamErrorPayload { message: string; }

/* —— 知识库 —— */
export interface KnowledgeBaseVO {
  id: number;
  name: string;
  description?: string;
  status: 'ENABLED' | 'DISABLED';
  statusName: string;
  documentCount?: number;
  createTime: string;
  updateTime: string;
}

export interface KnowledgeDocumentVO {
  id: number;
  knowledgeBaseId: number;
  knowledgeBaseName?: string;
  originalFilename: string;
  fileSize: number;
  fileSizeText?: string;
  fileType: string;
  status: string;
  statusName: string;
  chunkCount?: number;
  parsed?: boolean;
  parsedContentLength?: number;
  createTime: string;
}

/* —— AI 日志 —— */
export interface AiToolCallVO {
  id: number;
  sessionId: number;
  userMessageId?: number;
  toolName: string;
  toolNameText?: string;
  requestJson: string;
  requestPreview?: string;
  responseJson?: string;
  responsePreview?: string;
  status: string;
  statusName?: string;
  costMillis: number;
  createTime: string;
}

export interface AiRetrievalLogVO {
  id: number;
  sessionId: number;
  userMessageId?: number;
  questionText?: string;
  questionPreview?: string;
  documentId?: number;
  documentName?: string;
  chunkId?: number;
  chunkContentPreview?: string;
  score?: number;
  rankNo?: number;
  createTime: string;
}

export interface AiChatSessionAdminVO {
  id: number;
  userId: number;
  username?: string;
  nickname?: string;
  title: string;
  status?: string;
  statusName?: string;
  messageCount?: number;
  createTime: string;
  updateTime: string;
}

export interface AiTraceVO {
  session: AiChatSessionAdminVO;
  messages: ChatMessageVO[];
  retrievalLogs: AiRetrievalLogVO[];
  toolCallLogs: AiToolCallVO[];
}
