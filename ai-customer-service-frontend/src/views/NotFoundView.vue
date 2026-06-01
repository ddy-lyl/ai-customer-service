<script setup lang="ts">
import { useRouter } from 'vue-router';
const router = useRouter();
</script>

<template>
  <div class="not-found">
    <span class="blob blob--1"></span>
    <span class="blob blob--2"></span>
    <div class="card">
      <div class="emoji">🐳</div>
      <h1>404</h1>
      <p>这个页面被海风吹走啦，<br/>试试回到首页继续吧。</p>
      <button class="btn btn--primary" @click="router.push('/')">回到首页</button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.blob--1 { top: -120px; left: -120px; width: 380px; height: 380px; background: #BFE0F8; }
.blob--2 { bottom: -160px; right: -120px; width: 460px; height: 460px; background: #FBD7E4; }
.card {
  position: relative; z-index: 1;
  background: #fff;
  border-radius: var(--radius-xl);
  padding: 36px 48px;
  text-align: center;
  box-shadow: var(--shadow-lg);
}
.emoji { font-size: 48px; }
h1 { font-size: 42px; margin: 8px 0; color: var(--color-primary-600); }
p { color: var(--color-ink-500); margin-bottom: 18px; }
</style>
