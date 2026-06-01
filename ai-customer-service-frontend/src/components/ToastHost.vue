<script setup lang="ts">
import { useToast } from '@/composables/useToast';
import Icon from './Icon.vue';

const { toasts, remove } = useToast();

const ICON_MAP = {
  info: 'message',
  success: 'check',
  warning: 'lightbulb',
  error: 'close'
} as const;
</script>

<template>
  <transition-group tag="div" name="toast" class="toast-host">
    <div
      v-for="t in toasts"
      :key="t.id"
      :class="['toast', `toast--${t.type}`]"
      @click="remove(t.id)"
    >
      <span class="toast__icon"><Icon :name="ICON_MAP[t.type]" :size="16" /></span>
      <span class="toast__msg">{{ t.message }}</span>
    </div>
  </transition-group>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}
.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: var(--radius-pill);
  background: #fff;
  color: var(--color-ink-900);
  font-size: var(--fs-sm);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-soft);
  pointer-events: auto;
  cursor: pointer;
  min-width: 220px;
  max-width: 480px;
}
.toast__icon {
  width: 24px; height: 24px;
  border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff;
}
.toast--info    .toast__icon { background: var(--color-info); }
.toast--success .toast__icon { background: var(--color-success); }
.toast--warning .toast__icon { background: var(--color-warning); }
.toast--error   .toast__icon { background: var(--color-danger); }

.toast-enter-active,
.toast-leave-active { transition: all 280ms var(--ease-smooth); }
.toast-enter-from { opacity: 0; transform: translateY(-12px); }
.toast-leave-to   { opacity: 0; transform: translateY(-12px); }
</style>
