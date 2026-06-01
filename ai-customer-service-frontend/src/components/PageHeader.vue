<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>();
</script>

<template>
  <header class="page-header">
    <div>
      <h1 class="page-header__title">{{ title }}</h1>
      <p v-if="subtitle" class="page-header__sub">{{ subtitle }}</p>
    </div>
    <div class="page-header__extra">
      <slot name="extra" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.page-header__title {
  font-size: var(--fs-2xl);
  letter-spacing: -0.02em;
  font-weight: 600;
  background: linear-gradient(135deg, var(--color-primary-700), var(--color-primary-500) 70%, #9B8AD4);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.page-header__sub {
  margin-top: 4px;
  color: var(--color-ink-500);
  font-size: var(--fs-sm);
}
.page-header__extra { display: flex; gap: 10px; flex-wrap: wrap; }
</style>
