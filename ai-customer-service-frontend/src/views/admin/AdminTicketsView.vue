<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { adminUserApi } from '@/api/admin';
import { ticketApi } from '@/api/ticket';
import type { PageResult, TicketPriority, TicketSource, TicketStatus, TicketType, TicketVO, UserInfo } from '@/api/types';
import {
  TICKET_PRIORITY_MAP,
  TICKET_PRIORITY_OPTIONS,
  TICKET_SOURCE_MAP,
  TICKET_STATUS_MAP,
  TICKET_STATUS_OPTIONS,
  TICKET_TYPE_MAP,
  TICKET_TYPE_OPTIONS
} from '@/utils/enums';
import { formatTime, fromNow, shorten } from '@/utils/format';
import { useToast } from '@/composables/useToast';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import Modal from '@/components/Modal.vue';
import Pagination from '@/components/Pagination.vue';
import EmptyState from '@/components/EmptyState.vue';
import Icon from '@/components/Icon.vue';

const router = useRouter();
const toast = useToast();

const loading = ref(false);
const pageData = ref<PageResult<TicketVO>>({
  pageNo: 1, pageSize: 10, total: 0, pages: 1, records: []
});
const pageNo = ref(1);
const filter = reactive({
  status: '' as '' | TicketStatus,
  type: '' as '' | TicketType,
  priority: '' as '' | TicketPriority,
  source: '' as '' | TicketSource,
  keyword: ''
});

async function loadList() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { pageNo: pageNo.value, pageSize: 10 };
    if (filter.status) params.status = filter.status;
    if (filter.type) params.type = filter.type;
    if (filter.priority) params.priority = filter.priority;
    if (filter.source) params.source = filter.source;
    if (filter.keyword.trim()) params.keyword = filter.keyword.trim();
    pageData.value = await ticketApi.adminPage(params);
  } finally { loading.value = false; }
}

watch([pageNo], loadList);

function search() { pageNo.value = 1; loadList(); }
function reset() {
  filter.status = ''; filter.type = ''; filter.priority = ''; filter.source = ''; filter.keyword = '';
  search();
}

/* —— 分配工单 —— */
const staffList = ref<UserInfo[]>([]);
async function loadStaff() {
  try { staffList.value = await adminUserApi.staffList(); } catch { /* ignore */ }
}
const assignOpen = ref(false);
const assignTarget = ref<TicketVO | null>(null);
const assignStaffId = ref<number | null>(null);
const assignRemark = ref('');
function openAssign(t: TicketVO) {
  assignTarget.value = t;
  assignStaffId.value = t.staffId;
  assignRemark.value = '';
  assignOpen.value = true;
}
async function submitAssign() {
  if (!assignTarget.value || !assignStaffId.value) return toast.warning('请选择客服');
  await ticketApi.adminAssign(assignTarget.value.id, {
    staffId: assignStaffId.value,
    remark: assignRemark.value.trim() || undefined
  });
  toast.success('已分配');
  assignOpen.value = false;
  await loadList();
}

function viewDetail(t: TicketVO) {
  router.push({ name: 'ticket-detail', params: { id: t.id } });
}
function viewAiTrace(t: TicketVO) {
  if (t.sessionId) {
    router.push({ name: 'admin-ai-logs', query: { session: t.sessionId } });
  }
}

onMounted(() => {
  loadStaff();
  loadList();
});
</script>

<template>
  <div>
    <PageHeader title="工单管理" subtitle="全平台工单 · 仅对未分配工单分配客服（管理员不处理工单）" />

    <div class="filter-bar card">
      <div class="search-box">
        <Icon name="search" :size="14" />
        <input
          v-model.trim="filter.keyword"
          class="search-box__input"
          placeholder="按工单号 / 标题 / 描述"
          @keyup.enter="search"
        />
      </div>
      <select v-model="filter.status" class="select" style="width: 140px">
        <option value="">全部状态</option>
        <option v-for="o in TICKET_STATUS_OPTIONS" :key="o.value" :value="o.value">
          {{ o.label }}
        </option>
      </select>
      <select v-model="filter.type" class="select" style="width: 140px">
        <option value="">全部类型</option>
        <option v-for="o in TICKET_TYPE_OPTIONS" :key="o.value" :value="o.value">
          {{ o.label }}
        </option>
      </select>
      <select v-model="filter.priority" class="select" style="width: 130px">
        <option value="">全部优先级</option>
        <option v-for="o in TICKET_PRIORITY_OPTIONS" :key="o.value" :value="o.value">
          {{ o.label }}
        </option>
      </select>
      <select v-model="filter.source" class="select" style="width: 130px">
        <option value="">全部来源</option>
        <option value="USER">用户</option>
        <option value="AI">AI 创建</option>
        <option value="ADMIN">管理员</option>
      </select>
      <button class="btn btn--primary" @click="search">搜索</button>
      <button class="btn btn--ghost" @click="reset">重置</button>
    </div>

    <div class="card" style="margin-top: 16px; overflow: hidden">
      <div v-if="loading" class="loading-row">
        <div class="spinner"></div>
        <span style="color: var(--color-ink-500)">加载中…</span>
      </div>
      <EmptyState v-else-if="pageData.records.length === 0" title="未找到工单" icon="ticket" />
      <table v-else class="table">
        <thead>
          <tr>
            <th>工单 / 标题</th>
            <th>状态</th>
            <th>类型</th>
            <th>优先级</th>
            <th>来源</th>
            <th>用户</th>
            <th>客服</th>
            <th>创建时间</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in pageData.records" :key="t.id">
            <td>
              <div class="text-mono text-faint" style="font-size: var(--fs-xs)">{{ t.ticketNo }}</div>
              <div style="font-weight: 600">{{ t.title }}</div>
              <div v-if="t.orderNo" class="text-faint" style="font-size: var(--fs-xs); margin-top: 2px">
                订单 {{ t.orderNo }}
              </div>
            </td>
            <td><StatusChip :tone="TICKET_STATUS_MAP[t.status].tone" dot>{{ TICKET_STATUS_MAP[t.status].label }}</StatusChip></td>
            <td><StatusChip :tone="TICKET_TYPE_MAP[t.type].tone">{{ TICKET_TYPE_MAP[t.type].label }}</StatusChip></td>
            <td><StatusChip :tone="TICKET_PRIORITY_MAP[t.priority].tone">{{ TICKET_PRIORITY_MAP[t.priority].label }}</StatusChip></td>
            <td>
              <StatusChip :tone="TICKET_SOURCE_MAP[t.source].tone">
                <Icon v-if="t.source === 'AI'" name="sparkles" :size="11" />
                {{ TICKET_SOURCE_MAP[t.source].label }}
              </StatusChip>
            </td>
            <td>{{ t.userNickname }}</td>
            <td>{{ t.staffNickname || '—' }}</td>
            <td class="text-faint" style="font-size: var(--fs-xs)">{{ fromNow(t.createTime) }}</td>
            <td style="text-align: right; white-space: nowrap">
              <button class="btn btn--text btn--sm" @click="viewDetail(t)">详情</button>
              <button
                v-if="!t.staffId && t.status === 'PENDING'"
                class="btn btn--text btn--sm"
                @click="openAssign(t)"
              >
                分配客服
              </button>
              <span v-else-if="t.staffNickname" class="text-faint" style="font-size: var(--fs-xs)">
                已分配给 {{ t.staffNickname }}
              </span>
              <button
                v-if="t.sessionId"
                class="btn btn--text btn--sm"
                :title="`查看 AI 会话 #${t.sessionId}`"
                @click="viewAiTrace(t)"
              >
                <Icon name="sparkles" :size="12" />
              </button>
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

    <Modal v-model="assignOpen" :title="`分配工单 · ${assignTarget?.ticketNo || ''}`" width="480px">
      <div class="field">
        <label class="field__label">选择客服</label>
        <select v-model="assignStaffId" class="select">
          <option :value="null" disabled>请选择</option>
          <option v-for="s in staffList" :key="s.userId" :value="s.userId">
            {{ s.nickname || s.username }} (@{{ s.username }})
          </option>
        </select>
      </div>
      <div class="field">
        <label class="field__label">备注</label>
        <input v-model="assignRemark" class="input" placeholder="可选 · 给客服的说明" />
      </div>
      <template #footer>
        <button class="btn btn--ghost" @click="assignOpen = false">取消</button>
        <button class="btn btn--primary" @click="submitAssign">确认分配</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; flex-wrap: wrap;
}
.search-box {
  display: inline-flex; align-items: center; gap: 8px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  padding: 0 12px;
  height: 38px;
  color: var(--color-ink-400);
  flex: 1; min-width: 240px;
}
.search-box__input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: var(--fs-sm);
}
.loading-row { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 60px 0; }
</style>
