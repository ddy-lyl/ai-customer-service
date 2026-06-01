<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { liveChatApi } from '@/api/liveChat';
import type { ChatMessageVO, ChatServiceMode, LiveChatSessionVO } from '@/api/types';
import { useToast } from '@/composables/useToast';
import { useUserStore } from '@/stores/user';
import { connectLiveChatWs } from '@/utils/liveChatWs';
import Icon from '@/components/Icon.vue';
import Avatar from '@/components/Avatar.vue';
import EmptyState from '@/components/EmptyState.vue';
import { formatTime } from '@/utils/format';

const toast = useToast();
const userStore = useUserStore();

const tab = ref<'waiting' | 'mine'>('waiting');
const waiting = ref<LiveChatSessionVO[]>([]);
const mine = ref<LiveChatSessionVO[]>([]);
const loadingList = ref(false);
const currentSessionId = ref<number | null>(null);
const messages = ref<ChatMessageVO[]>([]);
const input = ref('');
const sending = ref(false);
const lastMessageId = ref(0);
let liveWs: WebSocket | null = null;

const currentItem = computed(() => {
  const list = tab.value === 'waiting' ? waiting.value : mine.value;
  return list.find((s) => s.sessionId === currentSessionId.value) ?? null;
});

async function loadLists() {
  loadingList.value = true;
  try {
    const [w, m] = await Promise.all([
      liveChatApi.staffWaiting({ pageNo: 1, pageSize: 50 }),
      liveChatApi.staffMine({ pageNo: 1, pageSize: 50 })
    ]);
    waiting.value = w.records;
    mine.value = m.records;
  } finally {
    loadingList.value = false;
  }
}

async function loadMessages(full = false) {
  if (!currentSessionId.value) return;
  const data = await liveChatApi.staffPollMessages(
    currentSessionId.value,
    full ? undefined : lastMessageId.value || undefined
  );
  if (full) {
    messages.value = data;
  } else if (data.length) {
    messages.value = [...messages.value, ...data];
  }
  const maxId = messages.value.reduce((m, x) => Math.max(m, x.id), 0);
  lastMessageId.value = maxId;
  await scrollToBottom();
}

async function selectSession(item: LiveChatSessionVO) {
  currentSessionId.value = item.sessionId;
  lastMessageId.value = 0;
  messages.value = [];
  stopLiveWs();
  await loadMessages(true);
  startLiveWs();
}

async function onAccept() {
  if (!currentSessionId.value || !currentItem.value) return;
  const version = currentItem.value.version ?? 0;
  try {
    await liveChatApi.staffAccept(currentSessionId.value, version);
    toast.success('已接入会话');
    await loadLists();
    tab.value = 'mine';
    await loadMessages(true);
    startLiveWs();
  } catch {
    toast.error('接入失败，会话可能已被其他客服抢占，请刷新列表');
    await loadLists();
  }
}

async function onEnd() {
  if (!currentSessionId.value) return;
  await liveChatApi.staffEnd(currentSessionId.value);
  toast.info('已结束人工接待');
  stopLiveWs();
  currentSessionId.value = null;
  messages.value = [];
  await loadLists();
}

async function onSend() {
  const text = input.value.trim();
  if (!text || !currentSessionId.value || sending.value) return;
  sending.value = true;
  try {
    const msg = await liveChatApi.staffSend(currentSessionId.value, { content: text });
    messages.value.push(msg);
    lastMessageId.value = Math.max(lastMessageId.value, msg.id);
    input.value = '';
    await scrollToBottom();
  } finally {
    sending.value = false;
  }
}

function startLiveWs() {
  stopLiveWs();
  if (!currentSessionId.value || !userStore.token) return;
  liveWs = connectLiveChatWs(currentSessionId.value, userStore.token, {
    onMessage: (m) => {
      if (messages.value.some((x) => x.id === m.id)) return;
      messages.value.push(m);
      lastMessageId.value = Math.max(lastMessageId.value, m.id);
      void scrollToBottom();
    },
    onSessionUpdate: () => {
      void loadLists();
    }
  });
}

function stopLiveWs() {
  if (liveWs) {
    liveWs.close();
    liveWs = null;
  }
}

const scroller = ref<HTMLDivElement | null>(null);
async function scrollToBottom() {
  await nextTick();
  if (scroller.value) {
    scroller.value.scrollTop = scroller.value.scrollHeight;
  }
}

function canReply(mode: ChatServiceMode | undefined) {
  return mode === 'HUMAN';
}

onMounted(() => {
  void loadLists();
});

onUnmounted(() => {
  stopLiveWs();
});
</script>

<template>
  <div class="live-shell">
    <aside class="live-aside">
      <div class="live-aside__tabs">
        <button
          :class="['live-tab', { active: tab === 'waiting' }]"
          @click="tab = 'waiting'"
        >
          待接入 ({{ waiting.length }})
        </button>
        <button :class="['live-tab', { active: tab === 'mine' }]" @click="tab = 'mine'">
          接待中 ({{ mine.length }})
        </button>
      </div>
      <div class="live-aside__list">
        <button
          v-for="s in tab === 'waiting' ? waiting : mine"
          :key="s.sessionId"
          :class="['live-item', { active: currentSessionId === s.sessionId }]"
          @click="selectSession(s)"
        >
          <div class="live-item__title">{{ s.userNickname || `用户#${s.userId}` }}</div>
          <div class="live-item__sub">{{ s.lastUserMessage || s.title }}</div>
          <div class="live-item__meta">
            <span class="chip chip--iris">{{ s.serviceModeName }}</span>
            <span v-if="s.activeTicketNo" class="live-item__tk">{{ s.activeTicketNo }}</span>
          </div>
        </button>
        <EmptyState
          v-if="!loadingList && (tab === 'waiting' ? waiting : mine).length === 0"
          title="暂无会话"
          :description="tab === 'waiting' ? '没有用户正在排队' : '您还没有接待中的会话'"
        />
      </div>
      <button class="btn btn--ghost live-refresh" @click="loadLists">
        <Icon name="refresh" :size="14" /> 刷新列表
      </button>
    </aside>

    <section class="live-main">
      <template v-if="currentSessionId && currentItem">
        <header class="live-main__head">
          <div>
            <h2>{{ currentItem.userNickname || `用户 #${currentItem.userId}` }}</h2>
            <p>会话 #{{ currentSessionId }} · {{ currentItem.serviceModeName }}</p>
          </div>
          <div class="live-main__actions">
            <button
              v-if="currentItem.serviceMode === 'WAITING_AGENT'"
              class="btn btn--primary"
              @click="onAccept"
            >
              接入会话
            </button>
            <button
              v-if="currentItem.serviceMode === 'HUMAN'"
              class="btn btn--ghost"
              @click="onEnd"
            >
              结束接待
            </button>
          </div>
        </header>

        <div ref="scroller" class="live-main__stream">
          <div
            v-for="m in messages"
            :key="m.id"
            :class="[
              'msg',
              m.role === 'USER' ? 'msg--user' : m.role === 'STAFF' ? 'msg--staff' : 'msg--sys'
            ]"
          >
            <Avatar
              :type="m.role === 'STAFF' ? 'staff' : m.role === 'USER' ? 'user' : 'ai'"
              :name="m.senderName || m.roleName"
              :size="32"
            />
            <div class="msg__body">
              <div class="msg__meta">
                {{ m.role === 'STAFF' ? m.senderName || '客服' : m.roleName }}
                <span class="msg__time">{{ formatTime(m.createTime, 'HH:mm:ss') }}</span>
              </div>
              <div class="bubble">{{ m.content }}</div>
            </div>
          </div>
        </div>

        <footer v-if="canReply(currentItem.serviceMode)" class="live-composer">
          <textarea v-model="input" rows="2" placeholder="输入回复，回车发送" @keydown.enter.prevent="onSend" />
          <button class="btn btn--primary" :disabled="!input.trim() || sending" @click="onSend">发送</button>
        </footer>
        <div v-else class="live-hint">请先点击「接入会话」后再回复用户</div>
      </template>
      <EmptyState v-else title="选择左侧会话" description="从待接入或接待中列表选择用户开始在线客服" />
    </section>
  </div>
</template>

<style scoped>
.live-shell {
  display: grid;
  grid-template-columns: 300px 1fr;
  height: calc(100vh - var(--layout-header-h) - 54px);
  min-height: 520px;
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.6);
  overflow: hidden;
}
.live-aside {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-soft);
  min-height: 0;
}
.live-aside__tabs {
  display: flex;
  gap: 4px;
  padding: 12px;
}
.live-tab {
  flex: 1;
  padding: 8px;
  border-radius: var(--radius-md);
  font-size: var(--fs-sm);
  background: transparent;
}
.live-tab.active {
  background: #fff;
  box-shadow: var(--shadow-sm);
  font-weight: 600;
}
.live-aside__list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}
.live-item {
  width: 100%;
  text-align: left;
  padding: 10px;
  border-radius: var(--radius-md);
  margin-bottom: 6px;
}
.live-item.active {
  background: rgba(169, 215, 242, 0.35);
}
.live-item__title {
  font-weight: 600;
  font-size: var(--fs-sm);
}
.live-item__sub {
  font-size: 12px;
  color: var(--color-ink-500);
  margin-top: 4px;
}
.live-item__meta {
  margin-top: 6px;
  display: flex;
  gap: 6px;
  align-items: center;
}
.live-item__tk {
  font-size: 11px;
  color: var(--color-ink-400);
}
.live-refresh {
  margin: 8px 12px 12px;
}
.live-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.live-main__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-soft);
}
.live-main__head h2 {
  margin: 0;
  font-size: 1.05rem;
}
.live-main__head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--color-ink-500);
}
.live-main__actions {
  display: flex;
  gap: 8px;
}
.live-main__stream {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
}
.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.msg--user {
  flex-direction: row-reverse;
}
.msg--user .msg__body {
  align-items: flex-end;
}
.msg__body {
  display: flex;
  flex-direction: column;
  max-width: 72%;
}
.msg__meta {
  font-size: 11px;
  color: var(--color-ink-500);
  margin-bottom: 4px;
}
.msg__time {
  margin-left: 6px;
}
.bubble {
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--border-soft);
  white-space: pre-wrap;
  line-height: 1.5;
}
.msg--user .bubble {
  background: linear-gradient(135deg, #a9d7f2, #9b8ad4);
  color: #1a2a3a;
  border: none;
}
.msg--staff .bubble {
  background: #e8f8f0;
  border-color: #bdeada;
}
.msg--sys .bubble {
  background: #fff8e8;
  font-size: 13px;
}
.live-composer {
  display: flex;
  gap: 10px;
  padding: 12px 18px;
  border-top: 1px solid var(--border-soft);
}
.live-composer textarea {
  flex: 1;
  resize: none;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-soft);
  padding: 10px;
}
.live-hint {
  padding: 14px;
  text-align: center;
  color: var(--color-ink-500);
  border-top: 1px solid var(--border-soft);
}
</style>
