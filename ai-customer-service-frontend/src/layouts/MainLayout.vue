<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterView, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';
import Icon from '@/components/Icon.vue';
import Avatar from '@/components/Avatar.vue';

const router = useRouter();
const user = useUserStore();
const toast = useToast();
const { open: confirm } = useConfirm();

const collapsed = ref<boolean>(localStorage.getItem('sidebar-collapsed') === '1');
const profileOpen = ref(false);

import { NAV_GROUPS } from '@/config/nav';

const groups = NAV_GROUPS;

const visibleGroups = computed(() =>
  groups
    .map((g) => ({
      ...g,
      items: g.items.filter((it) => it.roles.some((r) => user.roles.includes(r)))
    }))
    .filter((g) => g.items.length > 0)
);

const roleLabel = computed(() => {
  if (user.roles.includes('ADMIN')) return '管理员';
  if (user.roles.includes('STAFF')) return '客服';
  return '用户';
});

const roleTone = computed(() => {
  if (user.roles.includes('ADMIN')) return 'iris';
  if (user.roles.includes('STAFF')) return 'mint';
  return 'primary';
});

function toggleCollapse() {
  collapsed.value = !collapsed.value;
  localStorage.setItem('sidebar-collapsed', collapsed.value ? '1' : '0');
}

async function doLogout() {
  const ok = await confirm({
    title: '退出登录',
    message: '确认要退出当前账号吗？',
    confirmText: '退出',
    tone: 'danger'
  });
  if (!ok) return;
  await user.logout();
  toast.info('已退出，期待再见 🌈');
  router.replace({ name: 'login' });
}

onMounted(() => {
  if (user.token) user.fetchProfile();
});
</script>

<template>
  <div :class="['app-shell', { 'app-shell--collapsed': collapsed }]">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar__brand" @click="router.push('/chat')">
        <div class="sidebar__mark">
          <svg viewBox="0 0 48 48" width="28" height="28">
            <defs>
              <linearGradient id="m" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#A9D7F2"/>
                <stop offset="100%" stop-color="#6FA8DC"/>
              </linearGradient>
            </defs>
            <rect x="4" y="4" width="40" height="40" rx="12" fill="url(#m)"/>
            <circle cx="24" cy="20" r="7" fill="#fff" opacity="0.95"/>
            <circle cx="21" cy="19" r="1.3" fill="#3B6EA8"/>
            <circle cx="27" cy="19" r="1.3" fill="#3B6EA8"/>
            <path d="M18 32c2 2.5 10 2.5 12 0" stroke="#fff" stroke-width="2.2" stroke-linecap="round" fill="none"/>
          </svg>
        </div>
        <div v-show="!collapsed" class="sidebar__title">云屿售后</div>
      </div>

      <nav class="nav">
        <div v-for="g in visibleGroups" :key="g.title" class="nav__group">
          <div v-show="!collapsed" class="nav__group-title">{{ g.title }}</div>
          <RouterLink
            v-for="it in g.items"
            :key="it.to"
            :to="it.to"
            v-slot="{ isActive, navigate }"
            custom
          >
            <a
              :class="['nav__item', { active: isActive }]"
              :title="collapsed ? it.name : ''"
              @click="navigate"
            >
              <span class="nav__icon"><Icon :name="it.icon" :size="18" /></span>
              <span v-show="!collapsed" class="nav__label">{{ it.name }}</span>
              <span v-if="it.badge && !collapsed" class="nav__badge">{{ it.badge }}</span>
            </a>
          </RouterLink>
        </div>
      </nav>

      <button class="sidebar__collapse" :title="collapsed ? '展开' : '收起'" @click="toggleCollapse">
        <Icon :name="collapsed ? 'chevron-right' : 'chevron-left'" :size="14" />
      </button>
    </aside>

    <!-- 主区 -->
    <div class="main">
      <header class="topbar">
        <div class="topbar__crumb">
          <span class="topbar__home"><Icon name="home" :size="14" /></span>
          <span class="topbar__sep">/</span>
          <span class="topbar__page">{{ ($route.meta.title as string) ?? '工作台' }}</span>
        </div>

        <div class="topbar__actions">
          <div class="topbar__user" @click="profileOpen = !profileOpen">
            <Avatar :name="user.displayName" :type="user.isAdmin ? 'admin' : user.isStaff ? 'staff' : 'user'" :size="34" />
            <div class="topbar__user-text">
              <div class="topbar__user-name">
                {{ user.displayName }}
                <span :class="['chip', `chip--${roleTone}`]" style="margin-left: 6px">
                  {{ roleLabel }}
                </span>
              </div>
              <div class="topbar__user-account">@{{ user.username }}</div>
            </div>
            <Icon name="chevron-down" :size="14" />

            <Transition name="fade">
              <div v-if="profileOpen" class="topbar__menu" @click.stop>
                <div class="menu__head">
                  <Avatar :name="user.displayName" :type="user.isAdmin ? 'admin' : user.isStaff ? 'staff' : 'user'" :size="44" />
                  <div>
                    <div class="menu__name">{{ user.displayName }}</div>
                    <div class="menu__sub">{{ user.roles.join(' · ') }}</div>
                  </div>
                </div>
                <div class="menu__list">
                  <a class="menu__item" @click="profileOpen = false; doLogout()">
                    <Icon name="logout" :size="16" /> 退出登录
                  </a>
                </div>
              </div>
            </Transition>
          </div>
        </div>
      </header>

      <main class="content">
        <RouterView v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: var(--layout-sidebar-w) 1fr;
  min-height: 100vh;
  transition: grid-template-columns var(--transition-base);
}
.app-shell--collapsed {
  grid-template-columns: var(--layout-sidebar-w-collapsed) 1fr;
}

/* —— 侧边栏 —— */
.sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 18px 14px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px) saturate(140%);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
  border-right: 1px solid var(--border-soft);
}
.sidebar__brand {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 6px 18px;
  cursor: pointer;
}
.sidebar__mark {
  width: 40px; height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #DCEEFB, #C4DEF6);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 6px 14px rgba(80, 120, 180, 0.18);
}
.sidebar__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-ink-900);
  line-height: 1.2;
}

.nav {
  flex: 1;
  display: flex; flex-direction: column; gap: 18px;
  overflow-y: auto;
  margin: 0 -8px; padding: 0 8px;
}
.nav__group-title {
  font-size: 11px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  color: var(--color-ink-400);
  padding: 0 12px;
  margin-bottom: 6px;
}
.nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  margin-bottom: 4px;
  border-radius: var(--radius-md);
  color: var(--color-ink-700);
  font-size: var(--fs-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
  position: relative;
}
.nav__item:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}
.nav__icon {
  display: inline-flex; width: 24px; align-items: center; justify-content: center;
}
.nav__label { flex: 1; }
.nav__badge {
  background: linear-gradient(135deg, #FFD9C7, #FBD7E4);
  color: #B14855;
  padding: 1px 8px;
  font-size: 10px;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.nav__item.active {
  background: linear-gradient(135deg, rgba(169, 215, 242, 0.4), rgba(214, 204, 245, 0.4));
  color: var(--color-primary-700);
  box-shadow: inset 0 0 0 1px rgba(120, 160, 210, 0.18);
}
.nav__item.active::before {
  content: '';
  position: absolute; left: 0; top: 50%;
  width: 3px; height: 18px;
  background: linear-gradient(180deg, var(--color-primary-500), #9B8AD4);
  border-radius: 2px;
  transform: translateY(-50%);
}

.sidebar__collapse {
  position: absolute;
  top: 76px; right: -14px;
  width: 28px; height: 28px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid var(--border-soft);
  color: var(--color-ink-500);
  display: inline-flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm);
  z-index: 10;
}
.sidebar__collapse:hover {
  color: var(--color-primary-600);
  border-color: var(--color-primary-300);
}

/* —— 主区域 —— */
.main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100vh;
}

.topbar {
  height: var(--layout-header-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border-soft);
  position: sticky; top: 0; z-index: 50;
}
.topbar__crumb {
  display: flex; align-items: center; gap: 8px;
  color: var(--color-ink-500);
  font-size: var(--fs-sm);
}
.topbar__home {
  width: 28px; height: 28px;
  border-radius: 8px;
  background: var(--color-primary-50);
  color: var(--color-primary-600);
  display: inline-flex; align-items: center; justify-content: center;
}
.topbar__page { color: var(--color-ink-900); font-weight: 600; }

.topbar__user {
  position: relative;
  display: flex; align-items: center; gap: 10px;
  padding: 6px 12px;
  border-radius: var(--radius-pill);
  cursor: pointer;
  transition: background var(--transition-fast);
}
.topbar__user:hover { background: var(--color-primary-50); }
.topbar__user-text { line-height: 1.2; }
.topbar__user-name { font-size: var(--fs-sm); font-weight: 600; }
.topbar__user-account { font-size: 11px; color: var(--color-ink-400); }

.topbar__menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  width: 240px;
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-soft);
  overflow: hidden;
  z-index: 60;
}
.menu__head {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #EAF4FC, #F1ECFA);
}
.menu__name { font-weight: 600; }
.menu__sub { font-size: 11px; color: var(--color-ink-500); }
.menu__list { padding: 6px; }
.menu__item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--color-ink-700);
  font-size: var(--fs-sm);
  cursor: pointer;
}
.menu__item:hover { background: var(--color-primary-50); color: var(--color-primary-700); }

.content {
  flex: 1;
  padding: 26px 28px 28px;
  min-width: 0;
}

.fade-enter-active, .fade-leave-active { transition: all 200ms var(--ease-smooth); }
.fade-enter-from { opacity: 0; transform: translateY(-4px); }
.fade-leave-to   { opacity: 0; transform: translateY(-4px); }

@media (max-width: 880px) {
  .app-shell {
    grid-template-columns: var(--layout-sidebar-w-collapsed) 1fr !important;
  }
  .content { padding: 18px; }
  .topbar { padding: 0 16px; }
  .topbar__user-text { display: none; }
}
</style>
