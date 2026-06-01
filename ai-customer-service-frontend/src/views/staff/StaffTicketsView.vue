<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ticketApi } from '@/api/ticket';
import { orderApi } from '@/api/order';
import type { OrderVO, PageResult, TicketStatus, TicketVO } from '@/api/types';
import {
  TICKET_PRIORITY_MAP,
  TICKET_SOURCE_MAP,
  TICKET_STATUS_MAP,
  TICKET_TYPE_MAP,
  ORDER_STATUS_MAP
} from '@/utils/enums';
import { formatAmount, formatTime, fromNow } from '@/utils/format';
import { useToast } from '@/composables/useToast';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import EmptyState from '@/components/EmptyState.vue';
import Modal from '@/components/Modal.vue';
import Pagination from '@/components/Pagination.vue';
import Icon from '@/components/Icon.vue';

const toast = useToast();
const route = useRoute();

/** 由路由固定：POOL=待认领，MINE=我的处理（不再混用 ALL） */
const scopeMode = computed(() => (route.meta.scopeMode as 'POOL' | 'MINE') || 'POOL');
const isPoolMode = computed(() => scopeMode.value === 'POOL');

const pageTitle = computed(() => (isPoolMode.value ? '待认领工单' : '我的处理工单'));
const pageSubtitle = computed(() =>
  isPoolMode.value
    ? '仅展示未分配、待处理的工单 · 认领后进入「我的处理工单」'
    : '仅展示已分配给你的工单 · 可查看关联订单并提交处理结果'
);

const loading = ref(false);
const pageData = ref<PageResult<TicketVO>>({
  pageNo: 1, pageSize: 10, total: 0, pages: 1, records: []
});
const pageNo = ref(1);
const filterStatus = ref<'ALL' | TicketStatus>('ALL');
const claimingId = ref<number | null>(null);

async function loadList() {
  loading.value = true;
  try {
    pageData.value = await ticketApi.staffPage({
      pageNo: pageNo.value,
      pageSize: 10,
      scope: scopeMode.value,
      ...(filterStatus.value !== 'ALL' ? { status: filterStatus.value } : {})
    });
  } finally { loading.value = false; }
}

async function claimTicket(t: TicketVO) {
  claimingId.value = t.id;
  try {
    await ticketApi.staffClaim(t.id);
    toast.success('认领成功，请到「我的处理工单」跟进');
    await loadList();
  } finally {
    claimingId.value = null;
  }
}

watch([pageNo, filterStatus, scopeMode], () => {
  pageNo.value = 1;
  loadList();
});

/* 处理工单 */
const processOpen = ref(false);
const current = ref<TicketVO | null>(null);
const form = reactive({ processResult: '', remark: '', resolved: true });
const submitting = ref(false);

const orderInfo = ref<OrderVO | null>(null);
const orderLoading = ref(false);

async function openProcess(t: TicketVO) {
  current.value = t;
  form.processResult = '';
  form.remark = '';
  form.resolved = true;
  processOpen.value = true;
  orderInfo.value = null;
  if (t.orderNo) {
    orderLoading.value = true;
    try {
      orderInfo.value = await orderApi.staffDetail(t.orderNo);
    } catch { /* ignore */ }
    finally { orderLoading.value = false; }
  }
}

async function submitProcess() {
  if (!current.value) return;
  if (!form.processResult.trim()) return toast.warning('请填写处理结果');
  submitting.value = true;
  try {
    await ticketApi.staffProcess(current.value.id, {
      processResult: form.processResult.trim(),
      remark: form.remark.trim() || undefined,
      resolved: form.resolved
    });
    toast.success(form.resolved ? '工单已标记为已解决' : '处理已记录，仍在跟进中');
    processOpen.value = false;
    await loadList();
  } finally { submitting.value = false; }
}

const statusTabs = computed(() => [
  { key: 'ALL', label: '全部' },
  ...Object.entries(TICKET_STATUS_MAP).map(([key, meta]) => ({
    key: key as TicketStatus,
    label: meta.label
  }))
]);

const stats = computed(() => {
  const r = pageData.value.records;
  if (isPoolMode.value) {
    return {
      total: pageData.value.total,
      urgent: r.filter((t) => t.priority === 'URGENT' || t.priority === 'HIGH').length
    };
  }
  return {
    total: pageData.value.total,
    processing: r.filter((t) => t.status === 'PROCESSING').length,
    resolved: r.filter((t) => t.status === 'RESOLVED').length,
    urgent: r.filter((t) => t.priority === 'URGENT' || t.priority === 'HIGH').length
  };
});

onMounted(loadList);
</script>

<template>
  <div>
    <PageHeader :title="pageTitle" :subtitle="pageSubtitle">
      <template #extra>
        <button class="btn btn--ghost" @click="loadList">
          <Icon name="refresh" :size="14" /> 刷新
        </button>
      </template>
    </PageHeader>

    <div class="stat-row">
      <template v-if="isPoolMode">
        <div class="stat-card stat-card--blue">
          <div class="stat-card__icon"><Icon name="ticket" :size="18" /></div>
          <div>
            <div class="stat-card__num">{{ stats.total }}</div>
            <div class="stat-card__lbl">待认领总数</div>
          </div>
        </div>
        <div class="stat-card stat-card--peach">
          <div class="stat-card__icon"><Icon name="lightbulb" :size="18" /></div>
          <div>
            <div class="stat-card__num">{{ stats.urgent }}</div>
            <div class="stat-card__lbl">本页高优先级</div>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="stat-card stat-card--blue">
          <div class="stat-card__icon"><Icon name="tool" :size="18" /></div>
          <div>
            <div class="stat-card__num">{{ stats.total }}</div>
            <div class="stat-card__lbl">我的工单总数</div>
          </div>
        </div>
        <div class="stat-card stat-card--mint">
          <div class="stat-card__icon"><Icon name="tool" :size="18" /></div>
          <div>
            <div class="stat-card__num">{{ stats.processing }}</div>
            <div class="stat-card__lbl">本页处理中</div>
          </div>
        </div>
        <div class="stat-card stat-card--iris">
          <div class="stat-card__icon"><Icon name="check" :size="18" /></div>
          <div>
            <div class="stat-card__num">{{ stats.resolved }}</div>
            <div class="stat-card__lbl">本页已解决</div>
          </div>
        </div>
      </template>
    </div>

    <div class="tabs tabs--sub">
      <button
        v-for="t in statusTabs"
        :key="t.key"
        :class="['tab', { active: filterStatus === t.key }]"
        @click="filterStatus = t.key as TicketStatus | 'ALL'; pageNo = 1"
      >
        {{ t.label }}
      </button>
    </div>

    <div class="card" style="overflow: hidden">
      <div v-if="loading" class="loading-row">
        <div class="spinner"></div>
        <span style="color: var(--color-ink-500)">加载中…</span>
      </div>
      <EmptyState
        v-else-if="pageData.records.length === 0"
        :title="isPoolMode ? '暂无待认领工单' : '暂无分配给你的工单'"
        icon="tool"
      />
      <table v-else class="table">
        <thead>
          <tr>
            <th>工单号 / 标题</th>
            <th>状态</th>
            <th>类型</th>
            <th>优先级</th>
            <th>来源</th>
            <th>用户</th>
            <th>创建时间</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in pageData.records" :key="t.id">
            <td>
              <div class="text-mono" style="color: var(--color-ink-400); font-size: var(--fs-xs)">{{ t.ticketNo }}</div>
              <div style="font-weight: 600">{{ t.title }}</div>
              <div v-if="t.orderNo" class="text-faint" style="font-size: var(--fs-xs); margin-top: 4px">
                关联订单 {{ t.orderNo }}
              </div>
            </td>
            <td>
              <StatusChip :tone="TICKET_STATUS_MAP[t.status].tone" dot>
                {{ TICKET_STATUS_MAP[t.status].label }}
              </StatusChip>
            </td>
            <td>
              <StatusChip :tone="TICKET_TYPE_MAP[t.type].tone">
                {{ TICKET_TYPE_MAP[t.type].label }}
              </StatusChip>
            </td>
            <td>
              <StatusChip :tone="TICKET_PRIORITY_MAP[t.priority].tone">
                {{ TICKET_PRIORITY_MAP[t.priority].label }}
              </StatusChip>
            </td>
            <td>
              <StatusChip :tone="TICKET_SOURCE_MAP[t.source].tone">
                <Icon v-if="t.source === 'AI'" name="sparkles" :size="11" />
                {{ TICKET_SOURCE_MAP[t.source].label }}
              </StatusChip>
            </td>
            <td>{{ t.userNickname }}</td>
            <td class="text-faint" style="font-size: var(--fs-xs)">{{ fromNow(t.createTime) }}</td>
            <td style="text-align: right">
              <button
                v-if="isPoolMode"
                class="btn btn--text btn--sm"
                :disabled="claimingId === t.id"
                @click="claimTicket(t)"
              >
                <Icon name="plus" :size="13" /> 认领
              </button>
              <button
                v-else-if="t.status === 'PROCESSING'"
                class="btn btn--text btn--sm"
                @click="openProcess(t)"
              >
                <Icon name="tool" :size="13" /> 处理
              </button>
              <span v-else class="text-faint">{{ t.status === 'RESOLVED' ? '已解决' : '—' }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <Pagination
      v-if="pageData.total > 0"
      :page-no="pageData.pageNo"
      :page-size="pageData.pageSize"
      :total="pageData.total"
      @change="(p) => (pageNo = p)"
    />

    <Modal
      v-if="!isPoolMode"
      v-model="processOpen"
      :title="`处理工单 · ${current?.ticketNo || ''}`"
      width="640px"
    >
      <div v-if="current">
        <div class="proc-head">
          <h3>{{ current.title }}</h3>
          <div style="display: flex; gap: 6px; margin-top: 6px">
            <StatusChip :tone="TICKET_TYPE_MAP[current.type].tone">{{ TICKET_TYPE_MAP[current.type].label }}</StatusChip>
            <StatusChip :tone="TICKET_PRIORITY_MAP[current.priority].tone">{{ TICKET_PRIORITY_MAP[current.priority].label }}</StatusChip>
          </div>
          <p class="proc-desc">{{ current.description }}</p>
        </div>

        <div v-if="current.orderNo" class="proc-order">
          <div class="proc-order__head">
            <Icon name="order" :size="14" />
            <span>关联订单 {{ current.orderNo }}</span>
            <span v-if="orderLoading" class="spinner" style="width: 14px; height: 14px"></span>
          </div>
          <div v-if="orderInfo" class="proc-order__body">
            <div class="proc-order__item">
              <span class="proc-order__lbl">商品</span>
              <span>{{ orderInfo.productName }}</span>
            </div>
            <div class="proc-order__item">
              <span class="proc-order__lbl">订单金额</span>
              <span class="text-mono" style="color: var(--color-primary-700); font-weight: 600">
                {{ formatAmount(orderInfo.amount) }}
              </span>
            </div>
            <div class="proc-order__item">
              <span class="proc-order__lbl">订单状态</span>
              <StatusChip :tone="ORDER_STATUS_MAP[orderInfo.status].tone" dot>
                {{ ORDER_STATUS_MAP[orderInfo.status].label }}
              </StatusChip>
            </div>
            <div class="proc-order__item">
              <span class="proc-order__lbl">收货人</span>
              <span>{{ orderInfo.receiverName }} · {{ orderInfo.receiverPhone }}</span>
            </div>
          </div>
        </div>

        <div class="field" style="margin-top: 14px">
          <label class="field__label">处理结果 <span class="text-danger">*</span></label>
          <textarea
            v-model="form.processResult"
            class="textarea"
            placeholder="例如：已安排顺丰上门取件，预计 48 小时内到达仓库"
          ></textarea>
        </div>

        <div class="field">
          <label class="field__label">内部备注</label>
          <input v-model="form.remark" class="input" placeholder="可选，仅团队可见" />
        </div>

        <label class="check-row">
          <input type="checkbox" v-model="form.resolved" />
          <span class="check-row__box"><Icon v-if="form.resolved" name="check" :size="11" /></span>
          <span>本次处理后将工单标记为已解决</span>
        </label>
      </div>

      <template #footer>
        <button class="btn btn--ghost" @click="processOpen = false">取消</button>
        <button class="btn btn--primary" :disabled="submitting" @click="submitProcess">
          <span v-if="!submitting">提交处理</span>
          <span v-else class="spinner" style="border-color: rgba(255,255,255,0.4); border-top-color: #fff"></span>
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.loading-row { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 60px 0; }

.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}
.stat-card {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  background: #fff;
  border: 1px solid var(--border-card);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute; top: -20px; right: -20px;
  width: 90px; height: 90px;
  border-radius: 50%;
  opacity: 0.5;
}
.stat-card--blue::before  { background: #DCEEFB; }
.stat-card--peach::before { background: #FFD9C7; }
.stat-card--mint::before  { background: #BDEAD9; }
.stat-card--iris::before  { background: #D6CCF5; }

.stat-card__icon {
  position: relative; z-index: 1;
  width: 44px; height: 44px;
  border-radius: 12px;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--color-primary-400), var(--color-primary-600));
}
.stat-card--peach .stat-card__icon { background: linear-gradient(135deg, #FFB68C, #E69B7A); }
.stat-card--mint  .stat-card__icon { background: linear-gradient(135deg, #9CDDC1, #5FB89B); }
.stat-card--iris  .stat-card__icon { background: linear-gradient(135deg, #C0B0F0, #8E78D3); }
.stat-card__num {
  position: relative; z-index: 1;
  font-size: 24px; font-weight: 700;
  color: var(--color-ink-900);
  font-family: var(--font-mono);
}
.stat-card__lbl {
  position: relative; z-index: 1;
  font-size: var(--fs-xs);
  color: var(--color-ink-500);
}

.tabs {
  display: flex; gap: 6px; flex-wrap: wrap;
  background: rgba(255,255,255,0.65);
  padding: 6px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-soft);
  margin-bottom: 18px;
  width: fit-content;
}
.tab {
  height: 30px; padding: 0 14px;
  border-radius: 999px;
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
  cursor: pointer;
}
.tab.active {
  background: linear-gradient(135deg, var(--color-primary-400), var(--color-primary-600));
  color: #fff;
}

.proc-head h3 { font-size: var(--fs-md); font-weight: 600; }
.proc-desc {
  margin-top: 8px;
  padding: 10px 12px;
  background: var(--color-ink-50);
  border-radius: var(--radius-md);
  color: var(--color-ink-700);
  font-size: var(--fs-sm);
  line-height: var(--lh-loose);
  white-space: pre-wrap;
}

.proc-order {
  margin-top: 12px;
  padding: 12px 14px;
  background: linear-gradient(135deg, rgba(169,215,242,0.18), rgba(214,204,245,0.18));
  border-radius: var(--radius-md);
}
.proc-order__head {
  display: flex; align-items: center; gap: 6px;
  font-size: var(--fs-sm);
  color: var(--color-primary-700);
  font-weight: 500;
  margin-bottom: 10px;
}
.proc-order__body {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 12px;
}
.proc-order__item {
  display: flex; gap: 6px;
  font-size: var(--fs-sm);
}
.proc-order__lbl { color: var(--color-ink-500); width: 70px; flex-shrink: 0; }

.check-row {
  display: flex; align-items: center; gap: 8px;
  margin-top: 10px;
  font-size: var(--fs-sm);
  color: var(--color-ink-700);
  cursor: pointer; user-select: none;
}
.check-row input { display: none; }
.check-row__box {
  width: 18px; height: 18px;
  border-radius: 5px;
  border: 1px solid var(--border-strong);
  background: #fff;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff;
  transition: all var(--transition-fast);
}
.check-row input:checked ~ .check-row__box {
  background: var(--color-primary-500);
  border-color: var(--color-primary-500);
}
</style>
