<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { orderApi } from '@/api/order';
import type { OrderStatus, OrderVO, PageResult } from '@/api/types';
import { ORDER_STATUS_MAP } from '@/utils/enums';
import { formatAmount, formatTime, fromNow } from '@/utils/format';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import Modal from '@/components/Modal.vue';
import Pagination from '@/components/Pagination.vue';
import EmptyState from '@/components/EmptyState.vue';
import Icon from '@/components/Icon.vue';

const loading = ref(false);
const pageData = ref<PageResult<OrderVO>>({
  pageNo: 1,
  pageSize: 10,
  total: 0,
  pages: 1,
  records: []
});
const pageNo = ref(1);
const filter = reactive({
  orderNo: '',
  productName: '',
  status: '' as '' | OrderStatus
});

async function loadList() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { pageNo: pageNo.value, pageSize: 10 };
    if (filter.orderNo.trim()) params.orderNo = filter.orderNo.trim();
    if (filter.productName.trim()) params.productName = filter.productName.trim();
    if (filter.status) params.status = filter.status;
    pageData.value = await orderApi.adminPage(params);
  } finally {
    loading.value = false;
  }
}

watch([pageNo], loadList);

function search() {
  pageNo.value = 1;
  loadList();
}
function reset() {
  filter.orderNo = '';
  filter.productName = '';
  filter.status = '';
  search();
}

const detailOpen = ref(false);
const detail = ref<OrderVO | null>(null);
const detailLoading = ref(false);

async function openDetail(orderNo: string) {
  detailOpen.value = true;
  detail.value = null;
  detailLoading.value = true;
  try {
    detail.value = await orderApi.adminDetail(orderNo);
  } finally {
    detailLoading.value = false;
  }
}

onMounted(loadList);
</script>

<template>
  <div>
    <PageHeader title="订单管理" subtitle="全平台订单 · 核对用户售后关联订单 · 配合工单分配" />

    <div class="filter-bar card">
      <div class="search-box">
        <Icon name="search" :size="14" />
        <input
          v-model.trim="filter.orderNo"
          class="search-box__input"
          placeholder="订单号"
          @keyup.enter="search"
        />
      </div>
      <input
        v-model.trim="filter.productName"
        class="input"
        style="width: 180px"
        placeholder="商品名称"
        @keyup.enter="search"
      />
      <select v-model="filter.status" class="select" style="width: 140px">
        <option value="">全部状态</option>
        <option v-for="[key, meta] in Object.entries(ORDER_STATUS_MAP)" :key="key" :value="key">
          {{ meta.label }}
        </option>
      </select>
      <button class="btn btn--primary" @click="search">搜索</button>
      <button class="btn btn--ghost" @click="reset">重置</button>
    </div>

    <div class="card" style="margin-top: 16px; overflow: hidden">
      <div v-if="loading" class="loading-row">
        <div class="spinner"></div>
        <span style="color: var(--color-ink-500)">加载中…</span>
      </div>
      <EmptyState v-else-if="pageData.records.length === 0" title="未找到订单" icon="order" />
      <table v-else class="table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>商品</th>
            <th>用户ID</th>
            <th>金额</th>
            <th>状态</th>
            <th>支付时间</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in pageData.records" :key="o.id">
            <td class="text-mono">{{ o.orderNo }}</td>
            <td>
              <div style="font-weight: 600">{{ o.productName }}</div>
              <div class="text-faint" style="font-size: var(--fs-xs)">{{ o.productSku }}</div>
            </td>
            <td>{{ o.userId }}</td>
            <td>{{ formatAmount(o.amount) }}</td>
            <td>
              <StatusChip :tone="ORDER_STATUS_MAP[o.status].tone" dot>
                {{ ORDER_STATUS_MAP[o.status].label }}
              </StatusChip>
            </td>
            <td class="text-faint" style="font-size: var(--fs-xs)">
              {{ o.payTime ? fromNow(o.payTime) : '—' }}
            </td>
            <td style="text-align: right">
              <button class="btn btn--text btn--sm" @click="openDetail(o.orderNo)">详情</button>
            </td>
          </tr>
        </tbody>
      </table>
      <Pagination
        v-if="pageData.total > 0"
        v-model:page-no="pageNo"
        :total="pageData.total"
        :page-size="pageData.pageSize"
      />
    </div>

    <Modal v-model="detailOpen" title="订单详情" width="520px">
      <div v-if="detailLoading" class="loading-row"><div class="spinner"></div></div>
      <template v-else-if="detail">
        <dl class="detail-dl">
          <dt>订单号</dt><dd class="text-mono">{{ detail.orderNo }}</dd>
          <dt>状态</dt>
          <dd><StatusChip :tone="ORDER_STATUS_MAP[detail.status].tone">{{ detail.statusName }}</StatusChip></dd>
          <dt>商品</dt><dd>{{ detail.productName }}（{{ detail.productSku }}）</dd>
          <dt>金额</dt><dd>{{ formatAmount(detail.amount) }}</dd>
          <dt>用户ID</dt><dd>{{ detail.userId }}</dd>
          <dt>收货人</dt><dd>{{ detail.receiverName }} · {{ detail.receiverPhone }}</dd>
          <dt>地址</dt><dd>{{ detail.receiverAddress }}</dd>
          <dt>支付</dt><dd>{{ detail.payTime ? formatTime(detail.payTime) : '—' }}</dd>
          <dt>发货</dt><dd>{{ detail.shippingTime ? formatTime(detail.shippingTime) : '—' }}</dd>
        </dl>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 14px 16px;
}
.detail-dl {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 10px 16px;
  font-size: var(--fs-sm);
}
.detail-dl dt {
  color: var(--color-ink-500);
}
</style>
