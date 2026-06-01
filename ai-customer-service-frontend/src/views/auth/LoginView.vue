<script setup lang="ts">

import { reactive, ref } from 'vue';

import { useRoute, useRouter } from 'vue-router';

import { useUserStore } from '@/stores/user';

import { resolveHomePath } from '@/config/role-home';

import { useToast } from '@/composables/useToast';

import Icon from '@/components/Icon.vue';



const route = useRoute();

const router = useRouter();

const user = useUserStore();

const toast = useToast();



const form = reactive({

  username: '',

  password: ''

});

const showPwd = ref(false);

const loading = ref(false);



async function submit() {

  if (!form.username || !form.password) {

    toast.warning('请填写完整的用户名和密码');

    return;

  }

  loading.value = true;

  try {

    await user.login(form);

    toast.success(`欢迎回来，${user.displayName}`);

    const redirect = (route.query.redirect as string) || resolveHomePath(user.roles);

    router.replace(redirect);

  } catch { /* 已统一提示 */ }

  finally { loading.value = false; }

}

</script>



<template>

  <div class="auth-form">

    <div class="auth-form__head">

      <span class="dot dot--blue"></span>

      <span class="dot dot--mint"></span>

      <span class="dot dot--peach"></span>

    </div>

    <h1 class="auth-form__title">登录到 <span class="title-grad">云屿售后</span></h1>



    <form class="auth-form__body" @submit.prevent="submit">

      <div class="field">

        <label class="field__label">用户名</label>

        <div class="input-wrap">

          <span class="prefix"><Icon name="user" :size="16" /></span>

          <input

            v-model.trim="form.username"

            class="input input--with-prefix"

            placeholder="请输入用户名"

            autocomplete="username"

          />

        </div>

      </div>



      <div class="field">

        <label class="field__label">密码</label>

        <div class="input-wrap">

          <span class="prefix"><Icon name="lock" :size="16" /></span>

          <input

            v-model="form.password"

            :type="showPwd ? 'text' : 'password'"

            class="input input--with-prefix input--with-suffix"

            placeholder="请输入密码"

            autocomplete="current-password"

            @keyup.enter="submit"

          />

          <button

            type="button"

            class="suffix"

            tabindex="-1"

            @click="showPwd = !showPwd"

          >

            <Icon :name="showPwd ? 'eye-off' : 'eye'" :size="16" />

          </button>

        </div>

      </div>



      <button class="btn btn--primary auth-form__submit" :disabled="loading" type="submit">

        <span v-if="!loading">登录</span>

        <span v-else class="spinner spinner--white"></span>

      </button>

    </form>



    <div class="auth-form__footer">

      还没有账号？

      <RouterLink to="/register">立即注册</RouterLink>

    </div>

  </div>

</template>



<style scoped>

.auth-form {

  width: 100%;

  max-width: 380px;

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

.title-grad {

  background: linear-gradient(135deg, var(--color-primary-600), #9B8AD4);

  -webkit-background-clip: text;

  background-clip: text;

  color: transparent;

}



.auth-form__body {

  display: flex;

  flex-direction: column;

  gap: 16px;

  margin-top: 32px;

}



.input-wrap {

  position: relative;

}

.prefix, .suffix {

  position: absolute;

  top: 50%;

  transform: translateY(-50%);

  color: var(--color-ink-400);

  display: inline-flex;

  align-items: center;

}

.prefix { left: 14px; pointer-events: none; }

.suffix { right: 10px; padding: 4px; border-radius: 6px; cursor: pointer; }

.suffix:hover { color: var(--color-primary-600); background: var(--color-primary-50); }



.input--with-prefix { padding-left: 40px; }

.input--with-suffix { padding-right: 40px; }



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

</style>

