<script setup lang="ts">
import { computed } from 'vue';
import Icon from './Icon.vue';

const props = defineProps<{
  pageNo: number;
  pageSize: number;
  total: number;
}>();

const emit = defineEmits<{
  (e: 'change', page: number): void;
}>();

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));

const visiblePages = computed(() => {
  const total = totalPages.value;
  const cur = props.pageNo;
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages: (number | '...')[] = [1];
  if (cur > 4) pages.push('...');
  const start = Math.max(2, cur - 1);
  const end = Math.min(total - 1, cur + 1);
  for (let i = start; i <= end; i++) pages.push(i);
  if (cur < total - 3) pages.push('...');
  pages.push(total);
  return pages;
});

function go(p: number) {
  if (p < 1 || p > totalPages.value || p === props.pageNo) return;
  emit('change', p);
}
</script>

<template>
  <div v-if="total > 0" class="pager">
    <div class="pager__info">
      共 <b>{{ total }}</b> 条 · 第 {{ pageNo }} / {{ totalPages }} 页
    </div>
    <div class="pager__btns">
      <button class="pager__btn" :disabled="pageNo === 1" @click="go(pageNo - 1)">
        <Icon name="chevron-left" :size="14" />
      </button>
      <button
        v-for="(p, i) in visiblePages"
        :key="i"
        :class="['pager__btn', { active: p === pageNo, ellipsis: p === '...' }]"
        :disabled="p === '...'"
        @click="typeof p === 'number' && go(p)"
      >
        {{ p }}
      </button>
      <button class="pager__btn" :disabled="pageNo === totalPages" @click="go(pageNo + 1)">
        <Icon name="chevron-right" :size="14" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 4px 0;
  flex-wrap: wrap;
  gap: 10px;
}
.pager__info { font-size: var(--fs-sm); color: var(--color-ink-500); }
.pager__btns { display: flex; gap: 4px; }
.pager__btn {
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  border-radius: var(--radius-sm);
  background: #fff;
  border: 1px solid var(--border-soft);
  color: var(--color-ink-700);
  font-size: var(--fs-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.pager__btn:not(:disabled):hover {
  border-color: var(--color-primary-300);
  color: var(--color-primary-600);
}
.pager__btn.active {
  background: var(--color-primary-500);
  color: #fff;
  border-color: var(--color-primary-500);
  box-shadow: 0 4px 10px rgba(74, 156, 214, 0.3);
}
.pager__btn.ellipsis { background: transparent; border: none; color: var(--color-ink-400); }
.pager__btn:disabled { opacity: 0.45; cursor: not-allowed; }
</style>
