/**
 * 是否必须走同步 /ask（Function Calling）。
 * 仅在这些场景切同步；其余问题走 SSE 流式，保证打字机输出体验。
 */
const TOOL_PHRASES = [
  '查订单',
  '查询订单',
  '订单状态',
  '订单号',
  '查工单',
  '查询工单',
  '工单进度',
  '工单状态',
  '工单号',
  '创建工单',
  '提交工单',
  '申请售后',
  '申请退款',
  '申请退货',
  '转人工',
  '人工客服',
  '找客服',
  '列出待',
  '待认领',
  '我负责',
  '我的处理',
  '平台概览',
  '平台概况',
  '平台工单',
  '订单概况',
  '工单和订单',
  '运营概览',
  '热点问题',
  '工单统计',
  '订单统计',
  'ORD'
];

const ORDER_NO_REGEX = /\bORD\d{6,}\b/i;
const TICKET_NO_REGEX = /\bTK\d{6,}\b/i;

export function shouldUseSyncMode(question: string): boolean {
  if (!question) return false;
  if (ORDER_NO_REGEX.test(question)) return true;
  if (TICKET_NO_REGEX.test(question)) return true;
  return TOOL_PHRASES.some((phrase) => question.includes(phrase));
}
