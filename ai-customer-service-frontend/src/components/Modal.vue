<script setup lang="ts">
import Icon from './Icon.vue';

defineProps<{
  modelValue: boolean;
  title?: string;
  width?: string;
  hideClose?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'close'): void;
}>();

function close() {
  emit('update:modelValue', false);
  emit('close');
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="modelValue" class="overlay" @click.self="close">
        <div class="modal" :style="{ width: width ?? '520px' }">
          <div class="modal__header">
            <div class="modal__title">{{ title }}</div>
            <button v-if="!hideClose" class="modal__close" @click="close">
              <Icon name="close" :size="16" />
            </button>
          </div>
          <div class="modal__body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="modal__footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal__close {
  width: 28px; height: 28px;
  border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--color-ink-500);
  background: var(--color-ink-100);
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.modal__close:hover {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}
.fade-enter-active, .fade-leave-active { transition: opacity 180ms var(--ease-smooth); }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
