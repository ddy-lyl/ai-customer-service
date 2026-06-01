<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { adminUserApi } from '@/api/admin';
import type { PageResult, UserInfo } from '@/api/types';
import { formatTime } from '@/utils/format';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import Modal from '@/components/Modal.vue';
import Pagination from '@/components/Pagination.vue';
import EmptyState from '@/components/EmptyState.vue';
import Avatar from '@/components/Avatar.vue';
import Icon from '@/components/Icon.vue';

const toast = useToast();
const { open: confirm } = useConfirm();

const loading = ref(false);
const pageData = ref<PageResult<UserInfo>>({
  pageNo: 1, pageSize: 10, total: 0, pages: 1, records: []
});
const pageNo = ref(1);
const filter = reactive({ keyword: '', roleCode: '' as '' | 'USER' | 'STAFF' | 'ADMIN' });

async function loadList() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { pageNo: pageNo.value, pageSize: 10 };
    if (filter.keyword.trim()) {
      const kw = filter.keyword.trim();
      if (/^1\d{10}$/.test(kw)) params.phone = kw;
      else if (kw.includes('@')) params.email = kw;
      else { params.username = kw; params.nickname = kw; }
    }
    if (filter.roleCode) params.roleCode = filter.roleCode;
    pageData.value = await adminUserApi.page(params);
  } finally { loading.value = false; }
}
watch([pageNo, () => filter.roleCode], loadList);

function search() { pageNo.value = 1; loadList(); }

/* —— 创建客服 —— */
const createOpen = ref(false);
const createForm = reactive({ username: '', password: '', nickname: '', phone: '', email: '' });
const createLoading = ref(false);
function openCreate() {
  Object.assign(createForm, { username: '', password: '', nickname: '', phone: '', email: '' });
  createOpen.value = true;
}
async function submitCreate() {
  if (!createForm.username || !createForm.password || !createForm.nickname) {
    return toast.warning('请填写完整信息');
  }
  createLoading.value = true;
  try {
    await adminUserApi.createStaff(createForm);
    toast.success('客服账号已创建 ✨');
    createOpen.value = false;
    await loadList();
  } finally { createLoading.value = false; }
}

/* —— 重置密码 —— */
const pwdOpen = ref(false);
const pwdTarget = ref<UserInfo | null>(null);
const newPwd = ref('');
function openResetPwd(u: UserInfo) {
  pwdTarget.value = u;
  newPwd.value = '';
  pwdOpen.value = true;
}
async function submitReset() {
  if (!pwdTarget.value || newPwd.value.length < 6) {
    return toast.warning('密码至少 6 位');
  }
  await adminUserApi.resetPassword(pwdTarget.value.userId, { newPassword: newPwd.value });
  toast.success('密码已重置');
  pwdOpen.value = false;
}

/* —— 状态切换 —— */
async function toggleStatus(u: UserInfo) {
  const next = u.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  const ok = await confirm({
    title: next === 'DISABLED' ? '禁用账号' : '启用账号',
    message: `确认${next === 'DISABLED' ? '禁用' : '启用'} ${u.nickname || u.username} 吗？`,
    tone: next === 'DISABLED' ? 'danger' : 'primary'
  });
  if (!ok) return;
  await adminUserApi.updateStatus(u.userId, { status: next });
  toast.success('状态已更新');
  await loadList();
}

function primaryRole(u: UserInfo) {
  if (u.roles.includes('ADMIN')) return { label: '管理员', tone: 'iris', type: 'admin' as const };
  if (u.roles.includes('STAFF')) return { label: '客服', tone: 'mint', type: 'staff' as const };
  return { label: '用户', tone: 'primary', type: 'user' as const };
}

onMounted(loadList);
</script>

<template>
  <div>
    <PageHeader title="用户管理" subtitle="管理所有用户与客服账号 · 调整状态、重置密码">
      <template #extra>
        <button class="btn btn--primary" @click="openCreate">
          <Icon name="plus" :size="14" /> 创建客服
        </button>
      </template>
    </PageHeader>

    <div class="filter-bar card">
      <div class="search-box">
        <Icon name="search" :size="14" />
        <input
          v-model.trim="filter.keyword"
          class="search-box__input"
          placeholder="按用户名 / 昵称 / 手机号 / 邮箱"
          @keyup.enter="search"
        />
      </div>
      <select v-model="filter.roleCode" class="select" style="width: 160px">
        <option value="">全部角色</option>
        <option value="USER">用户</option>
        <option value="STAFF">客服</option>
        <option value="ADMIN">管理员</option>
      </select>
      <button class="btn btn--primary" @click="search">搜索</button>
      <button class="btn btn--ghost" @click="(filter.keyword = '', filter.roleCode = '', search())">
        重置
      </button>
    </div>

    <div class="card" style="margin-top: 16px; overflow: hidden">
      <div v-if="loading" class="loading-row">
        <div class="spinner"></div>
        <span style="color: var(--color-ink-500)">加载中…</span>
      </div>
      <EmptyState v-else-if="pageData.records.length === 0" title="未找到匹配的用户" icon="user" />
      <table v-else class="table">
        <thead>
          <tr>
            <th>用户</th>
            <th>角色</th>
            <th>手机号</th>
            <th>邮箱</th>
            <th>状态</th>
            <th style="text-align: right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in pageData.records" :key="u.userId">
            <td>
              <div style="display: flex; align-items: center; gap: 10px">
                <Avatar :name="u.nickname || u.username" :type="primaryRole(u).type" :size="34" />
                <div>
                  <div style="font-weight: 600">{{ u.nickname || u.username }}</div>
                  <div class="text-faint" style="font-size: var(--fs-xs)">@{{ u.username }}</div>
                </div>
              </div>
            </td>
            <td>
              <StatusChip :tone="primaryRole(u).tone as any">{{ primaryRole(u).label }}</StatusChip>
            </td>
            <td>{{ u.phone || '—' }}</td>
            <td>{{ u.email || '—' }}</td>
            <td>
              <StatusChip :tone="u.status === 'ENABLED' ? 'success' : 'danger'" dot>
                {{ u.status === 'ENABLED' ? '正常' : '已禁用' }}
              </StatusChip>
            </td>
            <td style="text-align: right">
              <button class="btn btn--text btn--sm" @click="openResetPwd(u)">
                <Icon name="lock" :size="12" /> 重置密码
              </button>
              <button class="btn btn--text btn--sm" @click="toggleStatus(u)">
                {{ u.status === 'ENABLED' ? '禁用' : '启用' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <Pagination
      v-if="pageData.total > 0"
      :page-no="pageData.pageNo"
      :page-size="pageData.pageSize"
      :total="pageData.total"
      @change="(p) => (pageNo = p)"
    />

    <Modal v-model="createOpen" title="创建客服账号" width="520px">
      <div class="row--2">
        <div class="field">
          <label class="field__label">用户名 *</label>
          <input v-model.trim="createForm.username" class="input" placeholder="登录账号" />
        </div>
        <div class="field">
          <label class="field__label">昵称 *</label>
          <input v-model.trim="createForm.nickname" class="input" placeholder="显示名" />
        </div>
      </div>
      <div class="field" style="margin-top: 12px">
        <label class="field__label">密码 *</label>
        <input v-model="createForm.password" type="password" class="input" placeholder="至少 6 位" />
      </div>
      <div class="row--2">
        <div class="field">
          <label class="field__label">手机号</label>
          <input v-model.trim="createForm.phone" class="input" />
        </div>
        <div class="field">
          <label class="field__label">邮箱</label>
          <input v-model.trim="createForm.email" class="input" />
        </div>
      </div>
      <template #footer>
        <button class="btn btn--ghost" @click="createOpen = false">取消</button>
        <button class="btn btn--primary" :disabled="createLoading" @click="submitCreate">
          创建
        </button>
      </template>
    </Modal>

    <Modal v-model="pwdOpen" title="重置密码" width="420px">
      <div class="text-muted" style="font-size: var(--fs-sm); margin-bottom: 8px">
        将为 <b>{{ pwdTarget?.nickname || pwdTarget?.username }}</b> 设置新密码
      </div>
      <div class="field">
        <label class="field__label">新密码</label>
        <input v-model="newPwd" type="password" class="input" placeholder="至少 6 位" />
      </div>
      <template #footer>
        <button class="btn btn--ghost" @click="pwdOpen = false">取消</button>
        <button class="btn btn--primary" @click="submitReset">确认</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  flex-wrap: wrap;
}
.search-box {
  display: inline-flex; align-items: center; gap: 8px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  padding: 0 12px;
  height: 38px;
  color: var(--color-ink-400);
  flex: 1; min-width: 260px;
}
.search-box__input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: var(--fs-sm); color: var(--color-ink-900);
}
.loading-row { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 60px 0; }
.row--2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
</style>
