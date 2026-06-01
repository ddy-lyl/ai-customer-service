import { defineStore } from 'pinia';
import { authApi } from '@/api/auth';
import type { LoginResult, RoleCode, UserInfo } from '@/api/types';

interface State {
  token: string;
  userId: number | null;
  username: string;
  nickname: string;
  roles: RoleCode[];
  profile: UserInfo | null;
}

function initial(): State {
  const raw = localStorage.getItem('userInfo');
  let cached: Partial<State> = {};
  if (raw) {
    try { cached = JSON.parse(raw); } catch { /* ignore */ }
  }
  return {
    token: localStorage.getItem('token') ?? '',
    userId: cached.userId ?? null,
    username: cached.username ?? '',
    nickname: cached.nickname ?? '',
    roles: cached.roles ?? [],
    profile: null
  };
}

export const useUserStore = defineStore('user', {
  state: (): State => initial(),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.roles.includes('ADMIN'),
    isStaff: (s) => s.roles.includes('STAFF') || s.roles.includes('ADMIN'),
    displayName: (s) => s.nickname || s.username || '游客',
    primaryRole: (s) => (
      s.roles.includes('ADMIN') ? 'ADMIN'
        : s.roles.includes('STAFF') ? 'STAFF'
          : 'USER'
    )
  },
  actions: {
    async login(payload: { username: string; password: string }) {
      const data = await authApi.login(payload);
      this.applyLoginResult(data);
      return data;
    },
    applyLoginResult(r: LoginResult) {
      const token = r.token?.trim() ?? '';
      this.token = token;
      this.userId = r.userId;
      this.username = r.username;
      this.nickname = r.nickname;
      this.roles = r.roles;
      localStorage.setItem('token', token);
      localStorage.setItem(
        'userInfo',
        JSON.stringify({
          userId: r.userId,
          username: r.username,
          nickname: r.nickname,
          roles: r.roles
        })
      );
    },
    async fetchProfile() {
      try {
        const me = await authApi.me();
        this.profile = me;
        this.nickname = me.nickname || this.nickname;
        this.roles = me.roles;
        localStorage.setItem(
          'userInfo',
          JSON.stringify({
            userId: me.userId,
            username: me.username,
            nickname: me.nickname,
            roles: me.roles
          })
        );
        return me;
      } catch {
        return null;
      }
    },
    async logout(silent = false) {
      try {
        if (!silent && this.token) await authApi.logout();
      } catch { /* ignore */ }
      this.$reset();
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
    }
  }
});
