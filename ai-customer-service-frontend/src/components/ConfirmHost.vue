<script setup lang="ts">
import { useConfirm } from '@/composables/useConfirm';

const { state, close } = useConfirm();
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="state.visible" class="overlay" @click.self="close(false)">
        <div class="modal" style="width: 420px">
          <div class="modal__header">
            <div class="modal__title">{{ state.title }}</div>
            <button class="btn btn--text" @click="close(false)">×</button>
          </div>
          <div class="modal__body" style="white-space: pre-wrap">{{ state.message }}</div>
          <div class="modal__footer">
            <button class="btn btn--ghost" @click="close(false)">
              {{ state.cancelText }}
            </button>
            <button
              :class="['btn', state.tone === 'danger' ? 'btn--danger' : 'btn--primary']"
              @click="close(true)"
            >
              {{ state.confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 180ms var(--ease-smooth); }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
