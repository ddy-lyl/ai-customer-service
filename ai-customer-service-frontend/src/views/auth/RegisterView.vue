<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { authApi } from '@/api/auth';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
});
const loading = ref(false);

async function submit() {
  if (!form.username || !form.password) {
    toast.warning('请填写用户名和密码');
    return;
  }
  if (form.password !== form.confirmPassword) {
    toast.warning('两次密码不一致');
    return;
  }
  loading.value = true;
  try {
    await authApi.register(form);
    toast.success('注册成功，欢迎登录');
    router.replace({ name: 'login', query: { username: form.username } });
  } catch { /* ignore */ }
  finally { loading.value = false; }
}
</script>

<template>
  <div class="auth-form auth-form--wide">
    <div class="auth-form__head">
      <span class="dot dot--blue"></span>
      <span class="dot dot--mint"></span>
      <span class="dot dot--peach"></span>
    </div>
    <h1 class="auth-form__title">创建账号</h1>

    <form class="auth-form__body" @submit.prevent="submit">
      <div class="row">
        <div class="field">
          <label class="field__label">用户名 *</label>
          <input v-model.trim="form.username" class="input" placeholder="登录账号" />
        </div>
        <div class="field">
          <label class="field__label">昵称</label>
          <input v-model.trim="form.nickname" class="input" placeholder="显示名" />
        </div>
      </div>

      <div class="field">
        <label class="field__label">密码 *</label>
        <input v-model="form.password" type="password" class="input" placeholder="至少 6 位" />
      </div>
      <div class="field">
        <label class="field__label">确认密码 *</label>
        <input v-model="form.confirmPassword" type="password" class="input" placeholder="再输入一次" />
      </div>

      <div class="row">
        <div class="field">
          <label class="field__label">手机号</label>
          <input v-model.trim="form.phone" class="input" placeholder="可选" />
        </div>
        <div class="field">
          <label class="field__label">邮箱</label>
          <input v-model.trim="form.email" class="input" placeholder="可选" />
        </div>
      </div>

      <button class="btn btn--primary auth-form__submit" :disabled="loading" type="submit">
        <span v-if="!loading">注册</span>
        <span v-else class="spinner spinner--white"></span>
      </button>
    </form>

    <div class="auth-form__footer">
      已经有账号？
      <RouterLink to="/login">直接登录</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.auth-form {
  width: 100%;
  max-width: 380px;
}
.auth-form--wide {
  max-width: 420px;
}
.auth-form__head {
  display: flex;
  gap: 6px;
  margin-bottom: 20px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.dot--blue  { background: #A9D7F2; }
.dot--mint  { background: #BDEAD9; }
.dot--peach { background: #FBD7E4; }

.auth-form__title {
  font-size: 26px;
  letter-spacing: -0.02em;
  font-weight: 700;
  color: var(--color-ink-900);
}

.auth-form__body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 32px;
}
.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.auth-form__submit {
  height: 44px;
  margin-top: 8px;
  font-weight: 600;
  font-size: var(--fs-md);
}
.spinner--white {
  border-color: rgba(255,255,255,0.4);
  border-top-color: #fff;
}

.auth-form__footer {
  margin-top: 28px;
  text-align: center;
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
}
.auth-form__footer a {
  color: var(--color-primary-600);
  font-weight: 500;
}

@media (max-width: 480px) {
  .row { grid-template-columns: 1fr; }
}
</style>
