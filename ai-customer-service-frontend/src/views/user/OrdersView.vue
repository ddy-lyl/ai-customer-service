<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { orderApi } from '@/api/order';
import type { OrderVO, OrderStatus } from '@/api/types';
import { ORDER_STATUS_MAP } from '@/utils/enums';
import { formatAmount, formatTime, fromNow } from '@/utils/format';
import { useToast } from '@/composables/useToast';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import Modal from '@/components/Modal.vue';
import EmptyState from '@/components/EmptyState.vue';
import Icon from '@/components/Icon.vue';

const toast = useToast();
const router = useRouter();

const loading = ref(false);
const list = ref<OrderVO[]>([]);
const search = ref('');
const filterStatus = ref<'ALL' | OrderStatus>('ALL');

const filtered = computed(() => {
  return list.value.filter((o) => {
    if (filterStatus.value !== 'ALL' && o.status !== filterStatus.value) return false;
    const kw = search.value.trim();
    if (!kw) return true;
    return (
      o.orderNo.includes(kw) ||
      o.productName.includes(kw) ||
      (o.productSku || '').includes(kw)
    );
  });
});

const statusTabs = computed(() => {
  const counts: Record<string, number> = { ALL: list.value.length };
  list.value.forEach((o) => {
    counts[o.status] = (counts[o.status] ?? 0) + 1;
  });
  return [
    { key: 'ALL', label: '全部', count: counts.ALL ?? 0 },
    ...Object.entries(ORDER_STATUS_MAP).map(([key, meta]) => ({
      key: key as OrderStatus,
      label: meta.label,
      count: counts[key] ?? 0
    }))
  ];
});

async function loadList() {
  loading.value = true;
  try {
    list.value = await orderApi.my();
  } finally {
    loading.value = false;
  }
}

const detailOpen = ref(false);
const detail = ref<OrderVO | null>(null);

async function openDetail(o: OrderVO) {
  detail.value = o;
  detailOpen.value = true;
  try {
    detail.value = await orderApi.detail(o.orderNo);
  } catch { /* ignore */ }
}

function gotoTicket(o: OrderVO) {
  router.push({ name: 'tickets', query: { orderNo: o.orderNo } });
}

onMounted(loadList);
</script>

<template>
  <div>
    <PageHeader title="我的订单" subtitle="所有售后流程都从这里开始 · 选择订单可申请退换货">
      <template #extra>
        <div class="search-box">
          <Icon name="search" :size="14" />
          <input
            v-model.trim="search"
            class="search-box__input"
            placeholder="搜索订单号 / 商品名"
          />
        </div>
        <button class="btn btn--ghost" @click="loadList">
          <Icon name="refresh" :size="14" /> 刷新
        </button>
      </template>
    </PageHeader>

    <div class="tabs">
      <button
        v-for="t in statusTabs"
        :key="t.key"
        :class="['tab', { active: filterStatus === t.key }]"
        @click="filterStatus = t.key as OrderStatus | 'ALL'"
      >
        {{ t.label }}
        <span class="tab__count">{{ t.count }}</span>
      </button>
    </div>

    <div v-if="loading" class="loading-row">
      <div class="spinner"></div>
      <span style="color: var(--color-ink-500)">加载中…</span>
    </div>

    <div v-else-if="filtered.length === 0" class="card">
      <EmptyState
        title="还没有相关订单"
        description="新订单到货后会出现在这里 · 也可以试试 AI 帮你查找"
      />
    </div>

    <div v-else class="orders-grid">
      <article
        v-for="o in filtered"
        :key="o.id"
        class="order-card"
        @click="openDetail(o)"
      >
        <div class="order-card__top">
          <div class="order-card__no">
            <Icon name="order" :size="14" />
            <span>{{ o.orderNo }}</span>
          </div>
          <StatusChip :tone="ORDER_STATUS_MAP[o.status].tone" dot>
            {{ ORDER_STATUS_MAP[o.status].label }}
          </StatusChip>
        </div>

        <h3 class="order-card__title">{{ o.productName }}</h3>
        <p v-if="o.productSku" class="order-card__sku">SKU · {{ o.productSku }}</p>

        <div class="order-card__amount">
          <span class="order-card__amount-lbl">订单金额</span>
          <span class="order-card__amount-val">{{ formatAmount(o.amount) }}</span>
        </div>

        <div class="order-card__meta">
          <div>
            <div class="meta__lbl">收货人</div>
            <div class="meta__val">{{ o.receiverName || '—' }}</div>
          </div>
          <div>
            <div class="meta__lbl">联系电话</div>
            <div class="meta__val">{{ o.receiverPhone || '—' }}</div>
          </div>
          <div>
            <div class="meta__lbl">下单时间</div>
            <div class="meta__val">{{ formatTime(o.createTime, 'MM-DD HH:mm') }}</div>
          </div>
        </div>

        <div class="order-card__foot">
          <span class="text-faint">{{ fromNow(o.updateTime || o.createTime) }} 更新</span>
          <div class="order-card__actions" @click.stop>
            <button class="btn btn--text btn--sm" @click="openDetail(o)">
              查看详情
            </button>
            <button class="btn btn--ghost btn--sm" @click="gotoTicket(o)">
              <Icon name="ticket" :size="13" /> 申请售后
            </button>
          </div>
        </div>
      </article>
    </div>

    <!-- 详情 Modal -->
    <Modal v-model="detailOpen" title="订单详情" width="560px">
      <div v-if="detail" class="detail">
        <div class="detail__head">
          <div>
            <div class="detail__no">{{ detail.orderNo }}</div>
            <StatusChip :tone="ORDER_STATUS_MAP[detail.status].tone" dot style="margin-top: 6px">
              {{ ORDER_STATUS_MAP[detail.status].label }}
            </StatusChip>
          </div>
          <div class="detail__amount">{{ formatAmount(detail.amount) }}</div>
        </div>

        <h3 style="margin-top: 14px">{{ detail.productName }}</h3>
        <div class="text-muted" style="font-size: var(--fs-sm)">SKU · {{ detail.productSku || '—' }}</div>

        <div class="detail__rows">
          <div class="detail__row">
            <span class="detail__lbl">收货人</span>
            <span>{{ detail.receiverName || '—' }} / {{ detail.receiverPhone || '—' }}</span>
          </div>
          <div class="detail__row">
            <span class="detail__lbl">收货地址</span>
            <span>{{ detail.receiverAddress || '—' }}</span>
          </div>
          <div class="detail__row">
            <span class="detail__lbl">备注</span>
            <span>{{ detail.remark || '—' }}</span>
          </div>
          <div class="detail__row">
            <span class="detail__lbl">下单时间</span>
            <span>{{ formatTime(detail.createTime) }}</span>
          </div>
          <div class="detail__row">
            <span class="detail__lbl">支付时间</span>
            <span>{{ formatTime(detail.payTime) }}</span>
          </div>
          <div class="detail__row">
            <span class="detail__lbl">发货时间</span>
            <span>{{ formatTime(detail.shippingTime) }}</span>
          </div>
          <div class="detail__row">
            <span class="detail__lbl">收货时间</span>
            <span>{{ formatTime(detail.receivedTime) }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <button class="btn btn--ghost" @click="detailOpen = false">关闭</button>
        <button v-if="detail" class="btn btn--primary" @click="detailOpen = false; gotoTicket(detail)">
          <Icon name="ticket" :size="14" /> 申请售后
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.search-box {
  display: inline-flex; align-items: center; gap: 8px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  padding: 0 12px;
  height: 36px;
  color: var(--color-ink-400);
  width: 260px;
}
.search-box__input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: var(--fs-sm); color: var(--color-ink-900);
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
  display: inline-flex; align-items: center; gap: 6px;
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
.tab__count {
  font-size: 11px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255,255,255,0.4);
  color: inherit;
  min-width: 18px; text-align: center;
}
.tab:not(.active) .tab__count {
  background: var(--color-ink-100);
  color: var(--color-ink-500);
}

.loading-row {
  display: flex; align-items: center; justify-content: center; gap: 10px;
  padding: 60px 0;
}

.orders-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.order-card {
  background: #fff;
  border: 1px solid var(--border-card);
  border-radius: var(--radius-lg);
  padding: 18px 18px 14px;
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
}
.order-card::before {
  content: '';
  position: absolute;
  top: 0; right: 0;
  width: 80px; height: 80px;
  background: radial-gradient(circle at top right, rgba(169,215,242,0.5) 0, transparent 70%);
  pointer-events: none;
}
.order-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-primary-200);
  box-shadow: var(--shadow-md);
}
.order-card__top {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 10px;
}
.order-card__no {
  display: inline-flex; align-items: center; gap: 6px;
  font-family: var(--font-mono);
  color: var(--color-ink-500);
  font-size: var(--fs-sm);
}
.order-card__title {
  font-size: var(--fs-md);
  font-weight: 600;
  margin-top: 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.order-card__sku {
  margin-top: 4px;
  font-size: var(--fs-xs);
  color: var(--color-ink-400);
  font-family: var(--font-mono);
}
.order-card__amount {
  margin-top: 12px;
  padding: 10px 12px;
  background: linear-gradient(135deg, rgba(169,215,242,0.18), rgba(214,204,245,0.18));
  border-radius: var(--radius-md);
  display: flex; align-items: baseline; justify-content: space-between;
}
.order-card__amount-lbl { font-size: var(--fs-xs); color: var(--color-ink-500); }
.order-card__amount-val {
  font-size: var(--fs-lg); font-weight: 700;
  color: var(--color-primary-700);
  font-family: var(--font-mono);
}
.order-card__meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
}
.meta__lbl { font-size: 11px; color: var(--color-ink-400); }
.meta__val {
  font-size: var(--fs-sm); color: var(--color-ink-700); margin-top: 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.order-card__foot {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-soft);
  display: flex; align-items: center; justify-content: space-between;
}
.order-card__actions { display: flex; gap: 4px; }

.detail__head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px;
}
.detail__no { font-family: var(--font-mono); font-size: var(--fs-md); color: var(--color-ink-700); }
.detail__amount {
  font-size: var(--fs-xl); font-weight: 700;
  color: var(--color-primary-700);
  font-family: var(--font-mono);
}
.detail__rows {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-soft);
  display: flex; flex-direction: column; gap: 8px;
}
.detail__row {
  display: flex; gap: 12px;
  font-size: var(--fs-sm);
  color: var(--color-ink-700);
}
.detail__lbl {
  flex-shrink: 0;
  width: 80px;
  color: var(--color-ink-500);
}
</style>
