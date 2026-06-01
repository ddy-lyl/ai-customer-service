<script setup lang="ts">
import { computed } from 'vue';
import type { ChipTone } from '@/utils/enums';

const props = withDefaults(
  defineProps<{
    tone?: ChipTone;
    dot?: boolean;
    outline?: boolean;
  }>(),
  { tone: 'primary', dot: false, outline: false }
);

const classList = computed(() => [
  'chip',
  `chip--${props.tone}`,
  { 'chip--outline': props.outline }
]);
</script>

<template>
  <span :class="classList">
    <span v-if="dot" class="dot"></span>
    <slot />
  </span>
</template>

<style scoped>
.chip--outline {
  background: transparent !important;
  border-color: currentColor;
}
</style>
