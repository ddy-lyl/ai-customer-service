<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ticketApi, type CreateTicketPayload } from '@/api/ticket';
import type { PageResult, TicketStatus, TicketVO } from '@/api/types';
import {
  TICKET_PRIORITY_MAP,
  TICKET_PRIORITY_OPTIONS,
  TICKET_SOURCE_MAP,
  TICKET_STATUS_MAP,
  TICKET_TYPE_MAP,
  TICKET_TYPE_OPTIONS
} from '@/utils/enums';
import { formatTime, fromNow, shorten } from '@/utils/format';
import { useToast } from '@/composables/useToast';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import EmptyState from '@/components/EmptyState.vue';
import Modal from '@/components/Modal.vue';
import Pagination from '@/components/Pagination.vue';
import Icon from '@/components/Icon.vue';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const loading = ref(false);
const pageData = ref<PageResult<TicketVO>>({
  pageNo: 1, pageSize: 10, total: 0, pages: 1, records: []
});
const pageNo = ref(1);
const filterStatus = ref<'ALL' | TicketStatus>('ALL');

async function loadList() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = {
      pageNo: pageNo.value,
      pageSize: 10
    };
    if (filterStatus.value !== 'ALL') params.status = filterStatus.value;
    pageData.value = await ticketApi.myPage(params);
  } finally {
    loading.value = false;
  }
}

watch([pageNo, filterStatus], loadList);

/* —— 创建工单 Modal —— */
const createOpen = ref(false);
const submitting = ref(false);
const form = reactive<CreateTicketPayload>({
  orderNo: '',
  title: '',
  type: 'REFUND',
  description: '',
  priority: 'MEDIUM'
});

function openCreate(prefillOrderNo?: string) {
  Object.assign(form, {
    orderNo: prefillOrderNo ?? '',
    title: '',
    type: 'REFUND',
    description: '',
    priority: 'MEDIUM'
  });
  createOpen.value = true;
}

async function submitCreate() {
  if (!form.title.trim()) return toast.warning('请填写工单标题');
  if (!form.description.trim()) return toast.warning('请描述你遇到的问题');
  submitting.value = true;
  try {
    const payload = { ...form, orderNo: form.orderNo?.trim() || null };
    await ticketApi.create(payload);
    toast.success('工单已提交，等待客服处理 ✨');
    createOpen.value = false;
    pageNo.value = 1;
    await loadList();
  } finally {
    submitting.value = false;
  }
}

const statusTabs = computed(() => {
  return [
    { key: 'ALL', label: '全部' },
    ...Object.entries(TICKET_STATUS_MAP).map(([key, meta]) => ({
      key: key as TicketStatus,
      label: meta.label
    }))
  ];
});

function gotoDetail(t: TicketVO) {
  router.push({ name: 'ticket-detail', params: { id: t.id } });
}

onMounted(() => {
  if (route.query.orderNo) {
    openCreate(String(route.query.orderNo));
  }
  loadList();
});
</script>

<template>
  <div>
    <PageHeader title="我的工单" subtitle="跟进每一个售后请求 · 由 AI 或人工客服为你处理">
      <template #extra>
        <button class="btn btn--primary" @click="openCreate()">
          <Icon name="plus" :size="14" /> 新建工单
        </button>
      </template>
    </PageHeader>

    <div class="tabs">
      <button
        v-for="t in statusTabs"
        :key="t.key"
        :class="['tab', { active: filterStatus === t.key }]"
        @click="filterStatus = t.key as TicketStatus | 'ALL'; pageNo = 1"
      >
        {{ t.label }}
      </button>
    </div>

    <div v-if="loading" class="loading-row">
      <div class="spinner"></div>
      <span style="color: var(--color-ink-500)">加载中…</span>
    </div>

    <div v-else-if="pageData.records.length === 0" class="card">
      <EmptyState
        title="还没有工单"
        description="遇到问题？点右上角「新建工单」或在 AI 对话里说一声就好"
        icon="ticket"
      >
        <button class="btn btn--primary" style="margin-top: 6px" @click="openCreate()">
          <Icon name="plus" :size="14" /> 立即创建
        </button>
      </EmptyState>
    </div>

    <ul v-else class="ticket-list">
      <li
        v-for="t in pageData.records"
        :key="t.id"
        class="ticket-item"
        @click="gotoDetail(t)"
      >
        <div class="ticket-item__bar" :class="`bar--${TICKET_STATUS_MAP[t.status].tone}`"></div>
        <div class="ticket-item__main">
          <div class="ticket-item__top">
            <span class="ticket-no">{{ t.ticketNo }}</span>
            <StatusChip :tone="TICKET_STATUS_MAP[t.status].tone" dot>
              {{ TICKET_STATUS_MAP[t.status].label }}
            </StatusChip>
            <StatusChip :tone="TICKET_TYPE_MAP[t.type].tone">
              {{ TICKET_TYPE_MAP[t.type].label }}
            </StatusChip>
            <StatusChip :tone="TICKET_PRIORITY_MAP[t.priority].tone">
              <Icon name="lightbulb" :size="11" />
              {{ TICKET_PRIORITY_MAP[t.priority].label }}
            </StatusChip>
            <StatusChip v-if="t.source === 'AI'" tone="iris">
              <Icon name="sparkles" :size="11" /> {{ TICKET_SOURCE_MAP[t.source].label }}
            </StatusChip>
          </div>
          <h3 class="ticket-title">{{ t.title }}</h3>
          <p class="ticket-desc">{{ shorten(t.description, 100) }}</p>

          <div class="ticket-foot">
            <span v-if="t.orderNo" class="meta-pill">
              <Icon name="order" :size="12" /> 订单 {{ t.orderNo }}
            </span>
            <span v-if="t.staffNickname" class="meta-pill">
              <Icon name="user" :size="12" /> 客服 {{ t.staffNickname }}
            </span>
            <span class="meta-pill meta-pill--ghost">
              创建于 {{ formatTime(t.createTime) }} · {{ fromNow(t.createTime) }}
            </span>
          </div>
        </div>
        <div class="ticket-item__chevron">
          <Icon name="chevron-right" :size="16" />
        </div>
      </li>
    </ul>

    <Pagination
      v-if="pageData.total > 0"
      :page-no="pageData.pageNo"
      :page-size="pageData.pageSize"
      :total="pageData.total"
      @change="(p) => (pageNo = p)"
    />

    <!-- 创建 Modal -->
    <Modal v-model="createOpen" title="新建工单" width="540px">
      <div class="field">
        <label class="field__label">工单标题 <span class="text-danger">*</span></label>
        <input v-model.trim="form.title" class="input" placeholder="一句话概括你遇到的问题" />
      </div>

      <div class="row">
        <div class="field">
          <label class="field__label">关联订单号</label>
          <input v-model.trim="form.orderNo" class="input" placeholder="可选 · 留空表示无关订单" />
          <span class="field__hint">必须是你自己的订单号，例如 202605180001</span>
        </div>
      </div>

      <div class="row row--2">
        <div class="field">
          <label class="field__label">类型</label>
          <select v-model="form.type" class="select">
            <option v-for="o in TICKET_TYPE_OPTIONS" :key="o.value" :value="o.value">
              {{ o.label }}
            </option>
          </select>
        </div>
        <div class="field">
          <label class="field__label">优先级</label>
          <select v-model="form.priority" class="select">
            <option v-for="o in TICKET_PRIORITY_OPTIONS" :key="o.value" :value="o.value">
              {{ o.label }}
            </option>
          </select>
        </div>
      </div>

      <div class="field">
        <label class="field__label">问题描述 <span class="text-danger">*</span></label>
        <textarea
          v-model="form.description"
          class="textarea"
          placeholder="请详细描述问题、出现的场景、希望如何处理"
        ></textarea>
      </div>

      <template #footer>
        <button class="btn btn--ghost" @click="createOpen = false">取消</button>
        <button class="btn btn--primary" :disabled="submitting" @click="submitCreate">
          <span v-if="!submitting">提交工单</span>
          <span v-else class="spinner" style="border-color: rgba(255,255,255,0.4); border-top-color: #fff"></span>
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
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
  height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.tab:hover { color: var(--color-primary-600); }
.tab.active {
  background: linear-gradient(135deg, var(--color-primary-400), var(--color-primary-600));
  color: #fff;
  box-shadow: 0 4px 10px rgba(74, 156, 214, 0.28);
}

.loading-row {
  display: flex; align-items: center; justify-content: center; gap: 10px;
  padding: 60px 0;
}

.ticket-list {
  display: flex; flex-direction: column; gap: 12px;
}
.ticket-item {
  display: flex;
  align-items: stretch;
  background: #fff;
  border: 1px solid var(--border-card);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  overflow: hidden;
}
.ticket-item:hover {
  border-color: var(--color-primary-200);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.ticket-item__bar {
  width: 4px;
  flex-shrink: 0;
  background: var(--color-ink-300);
}
.bar--success { background: var(--color-success); }
.bar--warning { background: var(--color-warning); }
.bar--primary { background: var(--color-primary-500); }
.bar--info    { background: var(--color-info); }
.bar--danger  { background: var(--color-danger); }
.bar--iris    { background: #9B8AD4; }

.ticket-item__main {
  flex: 1;
  padding: 16px 18px;
  min-width: 0;
}
.ticket-item__chevron {
  display: flex; align-items: center;
  padding: 0 18px;
  color: var(--color-ink-400);
}

.ticket-item__top {
  display: flex; align-items: center; gap: 8px;
  flex-wrap: wrap;
}
.ticket-no {
  font-family: var(--font-mono);
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
}
.ticket-title {
  margin-top: 8px;
  font-size: var(--fs-md);
  font-weight: 600;
}
.ticket-desc {
  margin-top: 4px;
  color: var(--color-ink-500);
  font-size: var(--fs-sm);
  line-height: var(--lh-base);
}
.ticket-foot {
  margin-top: 10px;
  display: flex; flex-wrap: wrap; gap: 8px;
  font-size: var(--fs-xs);
}
.meta-pill {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 10px;
  background: var(--color-primary-50);
  color: var(--color-primary-700);
  border-radius: 999px;
}
.meta-pill--ghost {
  background: var(--color-ink-100);
  color: var(--color-ink-500);
}

.row { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
.row--2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>
