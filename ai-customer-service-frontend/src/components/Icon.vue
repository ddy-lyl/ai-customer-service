<script setup lang="ts">
/* ============================================================================
 * Icon 组件 —— 内联 SVG，避免引入 icon-font 库。
 * 风格：圆头线条 + stroke-width=1.6，与马卡龙风格一致。
 * ============================================================================ */
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    name: string;
    size?: number | string;
    color?: string;
    strokeWidth?: number;
  }>(),
  { size: 18, strokeWidth: 1.6 }
);

const sizePx = computed(() =>
  typeof props.size === 'number' ? `${props.size}px` : props.size
);
</script>

<template>
  <svg
    :width="sizePx"
    :height="sizePx"
    viewBox="0 0 24 24"
    fill="none"
    :stroke="color ?? 'currentColor'"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    class="icon"
  >
    <!-- 按 name 渲染不同图标 -->
    <template v-if="name === 'sparkles'">
      <path d="M12 3l1.6 4.2L18 9l-4.4 1.8L12 15l-1.6-4.2L6 9l4.4-1.8z" />
      <path d="M19 14l.8 2.2L22 17l-2.2.8L19 20l-.8-2.2L16 17l2.2-.8z" />
    </template>

    <template v-else-if="name === 'message'">
      <path d="M21 12a8 8 0 1 1-3.2-6.4L21 4l-1.2 3.5A8 8 0 0 1 21 12z" />
      <circle cx="9" cy="12" r="1" />
      <circle cx="13" cy="12" r="1" />
      <circle cx="17" cy="12" r="1" />
    </template>

    <template v-else-if="name === 'send'">
      <path d="M22 2L11 13" />
      <path d="M22 2l-7 20-4-9-9-4 20-7z" />
    </template>

    <template v-else-if="name === 'order'">
      <path d="M4 4h12l4 4v12H4z" />
      <path d="M16 4v4h4" />
      <path d="M8 12h8M8 16h5" />
    </template>

    <template v-else-if="name === 'ticket'">
      <path d="M3 9a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4z" />
      <path d="M9 7v10" stroke-dasharray="2 2" />
    </template>

    <template v-else-if="name === 'user'">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 20c1.5-4 5-6 8-6s6.5 2 8 6" />
    </template>

    <template v-else-if="name === 'users'">
      <circle cx="9" cy="8" r="3.5" />
      <path d="M3 19c1.2-3.5 4-5 6-5s4.8 1.5 6 5" />
      <circle cx="17" cy="9" r="2.5" />
      <path d="M15 14c2.5 0 5 1.5 6 4" />
    </template>

    <template v-else-if="name === 'shield'">
      <path d="M12 3l8 3v6c0 4.5-3.5 8-8 9-4.5-1-8-4.5-8-9V6z" />
      <path d="M9 12l2 2 4-4" />
    </template>

    <template v-else-if="name === 'book'">
      <path d="M4 4h11a4 4 0 0 1 4 4v12H8a4 4 0 0 1-4-4z" />
      <path d="M4 16a4 4 0 0 1 4-4h11" />
    </template>

    <template v-else-if="name === 'logs'">
      <path d="M4 5h16M4 12h16M4 19h10" />
      <circle cx="20" cy="19" r="1.4" />
    </template>

    <template v-else-if="name === 'logout'">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="M16 17l5-5-5-5" />
      <path d="M21 12H9" />
    </template>

    <template v-else-if="name === 'plus'">
      <path d="M12 5v14M5 12h14" />
    </template>

    <template v-else-if="name === 'search'">
      <circle cx="11" cy="11" r="6" />
      <path d="M21 21l-4.3-4.3" />
    </template>

    <template v-else-if="name === 'close'">
      <path d="M6 6l12 12M18 6L6 18" />
    </template>

    <template v-else-if="name === 'check'">
      <path d="M5 12l5 5 9-11" />
    </template>

    <template v-else-if="name === 'chevron-down'">
      <path d="M6 9l6 6 6-6" />
    </template>

    <template v-else-if="name === 'chevron-left'">
      <path d="M15 6l-6 6 6 6" />
    </template>

    <template v-else-if="name === 'chevron-right'">
      <path d="M9 6l6 6-6 6" />
    </template>

    <template v-else-if="name === 'sidebar'">
      <rect x="3" y="4" width="18" height="16" rx="3" />
      <path d="M9 4v16" />
    </template>

    <template v-else-if="name === 'sun'">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </template>

    <template v-else-if="name === 'tool'">
      <path d="M14 3a4 4 0 0 1 4 6l-1 1 4 4-3 3-4-4-1 1a4 4 0 0 1-6-4l3-3" />
    </template>

    <template v-else-if="name === 'flow'">
      <circle cx="6" cy="6" r="2.5" />
      <circle cx="6" cy="18" r="2.5" />
      <circle cx="18" cy="12" r="2.5" />
      <path d="M8.5 6h4a3 3 0 0 1 3 3v0a3 3 0 0 0 3 3M15.5 12a3 3 0 0 1-3 3h-4" />
    </template>

    <template v-else-if="name === 'mail'">
      <rect x="3" y="5" width="18" height="14" rx="3" />
      <path d="M3 7l9 6 9-6" />
    </template>

    <template v-else-if="name === 'phone'">
      <path d="M22 16.92V21a1 1 0 0 1-1 1A19 19 0 0 1 2 3a1 1 0 0 1 1-1h4l2 5-2 1a13 13 0 0 0 7 7l1-2 5 2z" />
    </template>

    <template v-else-if="name === 'lock'">
      <rect x="4" y="11" width="16" height="10" rx="2" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </template>

    <template v-else-if="name === 'eye'">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z" />
      <circle cx="12" cy="12" r="3" />
    </template>

    <template v-else-if="name === 'eye-off'">
      <path d="M3 3l18 18" />
      <path d="M10.6 6.1A11 11 0 0 1 12 6c7 0 11 8 11 8a18 18 0 0 1-3.4 4M6.3 7.7A18 18 0 0 0 1 12s4 8 11 8c1.4 0 2.7-.3 3.9-.7" />
      <path d="M9.9 13.7A3 3 0 0 0 14 9.6" />
    </template>

    <template v-else-if="name === 'upload'">
      <path d="M12 16V4" />
      <path d="M7 9l5-5 5 5" />
      <path d="M4 20h16" />
    </template>

    <template v-else-if="name === 'file'">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6" />
    </template>

    <template v-else-if="name === 'refresh'">
      <path d="M21 12a9 9 0 0 1-15.4 6.4L3 16" />
      <path d="M3 12a9 9 0 0 1 15.4-6.4L21 8" />
      <path d="M3 21v-5h5M21 3v5h-5" />
    </template>

    <template v-else-if="name === 'home'">
      <path d="M3 11l9-8 9 8v9a2 2 0 0 1-2 2h-3v-7H10v7H5a2 2 0 0 1-2-2z" />
    </template>

    <template v-else-if="name === 'lightbulb'">
      <path d="M9 18h6M10 22h4" />
      <path d="M12 2a6 6 0 0 0-4 10.5c1 1 1.5 1.7 1.5 2.5h5c0-.8.5-1.5 1.5-2.5A6 6 0 0 0 12 2z" />
    </template>

    <template v-else-if="name === 'sparkle-dot'">
      <path d="M12 4l1.5 4.5L18 10l-4.5 1.5L12 16l-1.5-4.5L6 10l4.5-1.5z" />
    </template>

    <template v-else-if="name === 'doc-search'">
      <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
      <path d="M14 3v6h6" />
      <circle cx="11" cy="14" r="2" />
      <path d="M14 17l2 2" />
    </template>

    <template v-else>
      <!-- 默认占位 -->
      <circle cx="12" cy="12" r="8" />
    </template>
  </svg>
</template>

<style scoped>
.icon {
  flex-shrink: 0;
  display: inline-block;
  vertical-align: -3px;
}
</style>
