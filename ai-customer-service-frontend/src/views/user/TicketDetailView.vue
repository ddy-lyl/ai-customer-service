<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ticketApi } from '@/api/ticket';
import type { TicketFlowVO, TicketVO } from '@/api/types';
import {
  TICKET_PRIORITY_MAP,
  TICKET_SOURCE_MAP,
  TICKET_STATUS_MAP,
  TICKET_TYPE_MAP
} from '@/utils/enums';
import { formatTime, fromNow } from '@/utils/format';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';
import { useUserStore } from '@/stores/user';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import Modal from '@/components/Modal.vue';
import Icon from '@/components/Icon.vue';
import Avatar from '@/components/Avatar.vue';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const { open: confirm } = useConfirm();
const user = useUserStore();

const id = computed(() => Number(route.params.id));
const ticket = ref<TicketVO | null>(null);
const flows = ref<TicketFlowVO[]>([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    ticket.value = await ticketApi.detail(id.value);
    flows.value = ticket.value.flows ?? (await ticketApi.flows(id.value));
  } finally {
    loading.value = false;
  }
}

const closeOpen = ref(false);
const closeReason = ref('');
async function submitClose() {
  if (!closeReason.value.trim()) return toast.warning('请填写关闭原因');
  await ticketApi.close(id.value, { closeReason: closeReason.value.trim() });
  toast.success('工单已关闭');
  closeOpen.value = false;
  closeReason.value = '';
  await load();
}

const cancelOpen = ref(false);
const cancelReason = ref('');
async function submitCancel() {
  await ticketApi.cancel(id.value, {
    cancelReason: cancelReason.value.trim() || undefined
  });
  toast.success('工单已取消');
  cancelOpen.value = false;
  cancelReason.value = '';
  await load();
}

async function onCancelClick() {
  const ok = await confirm({
    title: '取消工单',
    message: '仅待处理状态的工单可取消，确定继续吗？',
    confirmText: '确认取消'
  });
  if (ok) cancelOpen.value = true;
}

const ACTION_MAP: Record<string, { label: string; icon: string; tone: string }> = {
  CREATE:  { label: '创建工单', icon: 'plus',    tone: 'primary' },
  ASSIGN:  { label: '分配处理', icon: 'user',    tone: 'info' },
  CLAIM:   { label: '认领工单', icon: 'user',    tone: 'info' },
  PROCESS: { label: '处理中',   icon: 'tool',    tone: 'warning' },
  RESOLVE: { label: '已解决',   icon: 'check',   tone: 'success' },
  CLOSE:   { label: '已关闭',   icon: 'close',   tone: 'info' },
  CANCEL:  { label: '已取消',   icon: 'close',   tone: 'danger' }
};

function gotoChat() {
  if (ticket.value?.sessionId) {
    router.push({ path: '/chat', query: { session: ticket.value.sessionId } });
  }
}

onMounted(load);
</script>

<template>
  <div>
    <PageHeader title="工单详情" subtitle="查看工单完整流转轨迹，必要时随时关闭">
      <template #extra>
        <button class="btn btn--ghost" @click="router.back()">
          <Icon name="chevron-left" :size="14" /> 返回列表
        </button>
        <button
          v-if="ticket?.status === 'PENDING' && user.primaryRole === 'USER'"
          class="btn btn--ghost"
          @click="onCancelClick"
        >
          <Icon name="close" :size="14" /> 取消工单
        </button>
        <button
          v-if="ticket && (ticket.status === 'RESOLVED' || ticket.status === 'PROCESSING' || ticket.status === 'PENDING')"
          class="btn btn--danger"
          @click="closeOpen = true"
        >
          <Icon name="close" :size="14" /> 关闭工单
        </button>
      </template>
    </PageHeader>

    <div v-if="loading" class="loading-row">
      <div class="spinner"></div>
      <span style="color: var(--color-ink-500)">加载中…</span>
    </div>

    <div v-else-if="ticket" class="detail-grid">
      <!-- 左：基本信息 + 描述 -->
      <div class="card detail-info">
        <div class="detail-info__head">
          <div>
            <div class="ticket-no">{{ ticket.ticketNo }}</div>
            <h2 class="ticket-title">{{ ticket.title }}</h2>
          </div>
          <StatusChip :tone="TICKET_STATUS_MAP[ticket.status].tone" dot>
            {{ TICKET_STATUS_MAP[ticket.status].label }}
          </StatusChip>
        </div>

        <div class="detail-info__tags">
          <StatusChip :tone="TICKET_TYPE_MAP[ticket.type].tone">
            {{ TICKET_TYPE_MAP[ticket.type].label }}
          </StatusChip>
          <StatusChip :tone="TICKET_PRIORITY_MAP[ticket.priority].tone">
            <Icon name="lightbulb" :size="11" />
            {{ TICKET_PRIORITY_MAP[ticket.priority].label }} 优先级
          </StatusChip>
          <StatusChip :tone="TICKET_SOURCE_MAP[ticket.source].tone">
            <Icon v-if="ticket.source === 'AI'" name="sparkles" :size="11" />
            {{ TICKET_SOURCE_MAP[ticket.source].label }}
          </StatusChip>
        </div>

        <div class="detail-info__meta">
          <div class="meta-cell">
            <div class="meta-cell__lbl">提交人</div>
            <div class="meta-cell__val">
              <Avatar :name="ticket.userNickname" :size="22" /> {{ ticket.userNickname }}
            </div>
          </div>
          <div class="meta-cell">
            <div class="meta-cell__lbl">处理客服</div>
            <div class="meta-cell__val">
              <template v-if="ticket.staffNickname">
                <Avatar :name="ticket.staffNickname" type="staff" :size="22" /> {{ ticket.staffNickname }}
              </template>
              <span v-else class="text-faint">尚未分配</span>
            </div>
          </div>
          <div class="meta-cell">
            <div class="meta-cell__lbl">关联订单</div>
            <div class="meta-cell__val">
              <span v-if="ticket.orderNo" class="text-mono">{{ ticket.orderNo }}</span>
              <span v-else class="text-faint">无</span>
            </div>
          </div>
          <div class="meta-cell">
            <div class="meta-cell__lbl">创建于</div>
            <div class="meta-cell__val">{{ formatTime(ticket.createTime) }}</div>
          </div>
        </div>

        <div class="block">
          <h3 class="block__title">问题描述</h3>
          <p class="block__text">{{ ticket.description }}</p>
        </div>

        <div v-if="ticket.processResult" class="block">
          <h3 class="block__title">处理结果</h3>
          <p class="block__text">{{ ticket.processResult }}</p>
        </div>

        <div v-if="ticket.closeReason" class="block">
          <h3 class="block__title">关闭原因</h3>
          <p class="block__text">{{ ticket.closeReason }}</p>
        </div>

        <div v-if="ticket.sessionId" class="ai-link" @click="gotoChat">
          <Icon name="sparkles" :size="14" />
          这条工单来自 AI 会话 #{{ ticket.sessionId }} · 查看对话上下文
          <Icon name="chevron-right" :size="13" />
        </div>
      </div>

      <!-- 右：时间线 -->
      <div class="card detail-flows">
        <h3 class="block__title" style="margin: 0 0 14px"><Icon name="flow" :size="14" /> 流转时间线</h3>
        <ol class="timeline">
          <li v-for="f in flows" :key="f.id" class="timeline__item">
            <span :class="['timeline__dot', `dot--${ACTION_MAP[f.action]?.tone || 'info'}`]">
              <Icon :name="ACTION_MAP[f.action]?.icon || 'flow'" :size="12" />
            </span>
            <div class="timeline__body">
              <div class="timeline__title">
                {{ f.actionName || ACTION_MAP[f.action]?.label || f.action }}
                <span class="timeline__time">{{ formatTime(f.createTime) }}</span>
              </div>
              <div class="timeline__sub">
                操作人：{{ f.operatorName || '系统' }} · {{ fromNow(f.createTime) }}
              </div>
              <div v-if="f.remark" class="timeline__remark">{{ f.remark }}</div>
            </div>
          </li>
        </ol>
      </div>
    </div>

    <Modal v-model="closeOpen" title="关闭工单" width="440px">
      <div class="field">
        <label class="field__label">关闭原因</label>
        <textarea
          v-model="closeReason"
          class="textarea"
          placeholder="说明问题是否解决 / 关闭的原因"
        ></textarea>
      </div>
      <template #footer>
        <button class="btn btn--ghost" @click="closeOpen = false">取消</button>
        <button class="btn btn--primary" @click="submitClose">确认关闭</button>
      </template>
    </Modal>

    <Modal v-model="cancelOpen" title="取消工单" width="440px">
      <div class="field">
        <label class="field__label">取消原因（可选）</label>
        <textarea
          v-model="cancelReason"
          class="textarea"
          placeholder="例如：问题已自行解决、误提交等"
        ></textarea>
      </div>
      <template #footer>
        <button class="btn btn--ghost" @click="cancelOpen = false">返回</button>
        <button class="btn btn--danger" @click="submitCancel">确认取消</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.loading-row {
  display: flex; align-items: center; justify-content: center; gap: 10px;
  padding: 60px 0;
}
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  align-items: start;
}
@media (max-width: 980px) {
  .detail-grid { grid-template-columns: 1fr; }
}

.detail-info { padding: 22px 24px; }
.detail-info__head {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
}
.ticket-no {
  font-family: var(--font-mono); font-size: var(--fs-sm);
  color: var(--color-ink-500);
}
.ticket-title {
  margin-top: 4px;
  font-size: var(--fs-xl);
  font-weight: 700;
  letter-spacing: -0.01em;
}
.detail-info__tags { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 14px; }
.detail-info__meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  margin-top: 18px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(169,215,242,0.15), rgba(214,204,245,0.15));
  border-radius: var(--radius-md);
}
.meta-cell__lbl { font-size: var(--fs-xs); color: var(--color-ink-500); }
.meta-cell__val {
  margin-top: 4px;
  display: inline-flex; align-items: center; gap: 6px;
  font-size: var(--fs-sm);
  color: var(--color-ink-900);
}

.block { margin-top: 22px; }
.block__title {
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
  font-weight: 600;
  letter-spacing: 0.5px;
  margin: 0 0 8px;
}
.block__text {
  font-size: var(--fs-md);
  color: var(--color-ink-700);
  white-space: pre-wrap;
  line-height: var(--lh-loose);
  padding: 12px 14px;
  background: var(--color-ink-50);
  border-radius: var(--radius-md);
}

.ai-link {
  margin-top: 20px;
  display: inline-flex; align-items: center; gap: 8px;
  background: linear-gradient(135deg, var(--bg-tint-iris), var(--bg-tint-blue));
  color: #6D5DB6;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: var(--fs-sm);
  cursor: pointer;
  border: 1px solid rgba(155, 138, 212, 0.3);
}
.ai-link:hover { filter: brightness(0.97); }

.detail-flows {
  padding: 22px 24px;
  position: sticky; top: calc(var(--layout-header-h) + 24px);
}
.timeline {
  position: relative;
  padding-left: 4px;
}
.timeline::before {
  content: '';
  position: absolute;
  left: 12px; top: 8px; bottom: 8px;
  width: 1px;
  background: var(--border-soft);
}
.timeline__item {
  position: relative;
  padding: 0 0 16px 36px;
}
.timeline__item:last-child { padding-bottom: 0; }
.timeline__dot {
  position: absolute;
  left: 0; top: 2px;
  width: 24px; height: 24px;
  border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff;
  z-index: 1;
}
.dot--primary { background: var(--color-primary-500); }
.dot--success { background: var(--color-success); }
.dot--warning { background: var(--color-warning); }
.dot--info    { background: var(--color-info); }
.dot--danger  { background: var(--color-danger); }

.timeline__title {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--color-ink-900);
}
.timeline__time {
  font-size: var(--fs-xs);
  color: var(--color-ink-400);
  font-weight: 400;
  margin-left: 8px;
}
.timeline__sub {
  margin-top: 2px;
  font-size: var(--fs-xs);
  color: var(--color-ink-500);
}
.timeline__remark {
  margin-top: 6px;
  font-size: var(--fs-xs);
  color: var(--color-ink-700);
  padding: 6px 10px;
  background: var(--color-ink-50);
  border-radius: var(--radius-sm);
}
</style>
