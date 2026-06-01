<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    name?: string;
    size?: number;
    type?: 'user' | 'ai' | 'staff' | 'admin';
  }>(),
  { size: 36, type: 'user', name: '' }
);

const palette = [
  ['#A9D7F2', '#6FA8DC'],
  ['#BDEAD9', '#7BC9A7'],
  ['#FBD7E4', '#E59FB8'],
  ['#FFF0B8', '#E3BD63'],
  ['#D6CCF5', '#9B8AD4'],
  ['#FFD9C7', '#E69B7A']
];

const gradient = computed(() => {
  if (props.type === 'ai') return ['#A9D7F2', '#9B8AD4'];
  if (props.type === 'admin') return ['#D6CCF5', '#7B9BD9'];
  if (props.type === 'staff') return ['#BDEAD9', '#6FA8DC'];
  const n = props.name || 'guest';
  const sum = [...n].reduce((s, c) => s + c.charCodeAt(0), 0);
  return palette[sum % palette.length];
});

const initial = computed(() => {
  if (props.type === 'ai') return '云';
  const n = props.name?.trim();
  if (!n) return 'U';
  return n.slice(0, 1).toUpperCase();
});

const style = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.round(props.size * 0.42)}px`,
  background: `linear-gradient(135deg, ${gradient.value[0]}, ${gradient.value[1]})`
}));
</script>

<template>
  <div class="avatar" :style="style">
    <slot>{{ initial }}</slot>
  </div>
</template>

<style scoped>
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 10px rgba(80, 120, 180, 0.18), inset 0 0 0 2px rgba(255, 255, 255, 0.5);
  user-select: none;
}
</style>
