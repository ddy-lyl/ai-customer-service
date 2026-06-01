<script setup lang="ts">
import { RouterView } from 'vue-router';
</script>

<template>
  <div class="auth-layout">
    <span class="blob blob--1"></span>
    <span class="blob blob--2"></span>
    <span class="blob blob--3"></span>
    <span class="grain"></span>

    <div class="auth-frame">
      <aside class="auth-brand">
        <div class="auth-brand__logo">
          <div class="brand-mark">
            <svg viewBox="0 0 48 48" width="36" height="36">
              <defs>
                <linearGradient id="lm" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#A9D7F2"/>
                  <stop offset="100%" stop-color="#6FA8DC"/>
                </linearGradient>
              </defs>
              <rect x="4" y="4" width="40" height="40" rx="12" fill="url(#lm)"/>
              <circle cx="24" cy="20" r="8" fill="#fff" opacity="0.95"/>
              <circle cx="21" cy="19" r="1.5" fill="#3B6EA8"/>
              <circle cx="27" cy="19" r="1.5" fill="#3B6EA8"/>
              <path d="M18 33c2 2.5 10 2.5 12 0" stroke="#fff" stroke-width="2.4" stroke-linecap="round" fill="none"/>
            </svg>
          </div>
          <div class="brand-title">云屿售后</div>
        </div>

        <div class="auth-tagline">
          <h2>把每一次售后<br />变成一次温柔的相遇</h2>
        </div>
      </aside>

      <main class="auth-main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
}

.blob--1 { top: -120px; left: -120px; width: 380px; height: 380px; background: #BFE0F8; }
.blob--2 { bottom: -160px; right: -120px; width: 460px; height: 460px; background: #D6CCF5; }
.blob--3 { top: 40%; right: 30%; width: 240px; height: 240px; background: #FBD7E4; opacity: 0.4; }

.grain {
  position: absolute; inset: 0;
  background-image:
    radial-gradient(circle at 25% 30%, rgba(255,255,255,0.5) 0, transparent 35%),
    radial-gradient(circle at 75% 70%, rgba(255,255,255,0.4) 0, transparent 40%);
  pointer-events: none;
}

.auth-frame {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: min(960px, 100%);
  min-height: 560px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(28px) saturate(150%);
  -webkit-backdrop-filter: blur(28px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(80, 120, 180, 0.18);
}

.auth-brand {
  position: relative;
  padding: 48px 44px;
  background: linear-gradient(160deg, #A9D7F2 0%, #6FA8DC 55%, #9B8AD4 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 56px;
  overflow: hidden;
}
.auth-brand::after {
  content: '';
  position: absolute; inset: 0;
  background:
    radial-gradient(circle at 20% 0%, rgba(255,255,255,0.35) 0, transparent 38%),
    radial-gradient(circle at 100% 100%, rgba(255,255,255,0.25) 0, transparent 45%);
  pointer-events: none;
}

.auth-brand__logo {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  z-index: 1;
}
.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
  flex-shrink: 0;
}
.brand-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.auth-tagline {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
}
.auth-tagline h2 {
  color: #fff;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.auth-main {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 56px;
  background: rgba(255, 255, 255, 0.6);
}

@media (max-width: 840px) {
  .auth-frame { grid-template-columns: 1fr; }
  .auth-brand {
    padding: 32px 28px;
    gap: 24px;
    min-height: 180px;
  }
  .auth-tagline { flex: none; }
  .auth-tagline h2 { font-size: 22px; }
  .auth-main { padding: 32px 24px; }
}
</style>
