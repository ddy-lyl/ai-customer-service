<script setup lang="ts">
import Icon from './Icon.vue';

defineProps<{
  title?: string;
  description?: string;
  icon?: string;
}>();
</script>

<template>
  <div class="empty">
    <div class="empty__icon">
      <Icon :name="icon ?? 'doc-search'" :size="32" />
    </div>
    <div>
      <div style="color: var(--color-ink-700); font-weight: 500; font-size: var(--fs-md)">
        {{ title ?? '暂无数据' }}
      </div>
      <div v-if="description" style="margin-top: 4px; font-size: var(--fs-sm)">
        {{ description }}
      </div>
    </div>
    <slot />
  </div>
</template>
