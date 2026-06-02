<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { chatApi, askStream, type AskPayload } from '@/api/chat';
import { liveChatApi } from '@/api/liveChat';
import type {
  ChatMessageVO,
  ChatServiceMode,
  ChatSessionDetailVO,
  ChatSessionVO,
  RetrievalSource
} from '@/api/types';
import { shouldUseSyncMode } from '@/utils/intent';
import { revealTextProgressively } from '@/utils/stream-reveal';
import { renderMarkdown } from '@/utils/markdown';
import { formatTime, fromNow, shorten } from '@/utils/format';
import { useToast } from '@/composables/useToast';
import { useUserStore } from '@/stores/user';
import { resolveChatConfig } from '@/config/chat-role';
import { connectLiveChatWs } from '@/utils/liveChatWs';
import Icon from '@/components/Icon.vue';
import Avatar from '@/components/Avatar.vue';
import EmptyState from '@/components/EmptyState.vue';

const user = useUserStore();
const toast = useToast();
const route = useRoute();

const chatConfig = computed(() => resolveChatConfig(user.roles));

/* —— 会话列表 —— */
const sessions = ref<ChatSessionVO[]>([]);
const loadingSessions = ref(false);
const currentSessionId = ref<number | null>(null);
const searchSession = ref('');

const filteredSessions = computed(() => {
  const kw = searchSession.value.trim();
  if (!kw) return sessions.value;
  return sessions.value.filter(
    (s) => (s.title || '').includes(kw) || (s.lastMessage || '').includes(kw)
  );
});

async function loadSessions() {
  loadingSessions.value = true;
  try {
    const res = await chatApi.mySessions({ pageNo: 1, pageSize: 50 });
    sessions.value = res.records;
  } finally {
    loadingSessions.value = false;
  }
}

/* —— 当前会话消息 —— */
interface DisplayMsg {
  id?: number | string;
  role: 'USER' | 'ASSISTANT' | 'STAFF' | 'SYSTEM';
  content: string;
  intent?: string | null;
  modelName?: string | null;
  senderName?: string | null;
  createTime?: string;
  sources?: RetrievalSource[];
  streaming?: boolean;
  error?: boolean;
}

const sessionDetail = ref<ChatSessionDetailVO | null>(null);
const lastPollMessageId = ref(0);
let liveWs: WebSocket | null = null;

const serviceMode = computed<ChatServiceMode>(
  () => sessionDetail.value?.serviceMode ?? 'AI'
);
const isLiveMode = computed(
  () => serviceMode.value === 'WAITING_AGENT' || serviceMode.value === 'HUMAN'
);
const isUserRole = computed(() => user.primaryRole === 'USER');

function mapApiMessage(m: ChatMessageVO): DisplayMsg {
  return {
    id: m.id,
    role: m.role as DisplayMsg['role'],
    content: m.content,
    intent: m.intent,
    modelName: m.modelName,
    senderName: m.senderName,
    createTime: m.createTime
  };
}

async function loadSessionDetail(sessionId: number) {
  try {
    sessionDetail.value = await liveChatApi.sessionDetail(sessionId);
  } catch {
    sessionDetail.value = null;
  }
}

function appendLiveMessage(m: ChatMessageVO) {
  if (messages.value.some((x) => x.id === m.id)) return;
  messages.value.push(mapApiMessage(m));
  lastPollMessageId.value = Math.max(lastPollMessageId.value, m.id);
  void scrollToBottom(false);
}

function startLiveChatWs() {
  stopLiveChatWs();
  if (!currentSessionId.value || !isLiveMode.value || !user.token) return;
  liveWs = connectLiveChatWs(currentSessionId.value, user.token, {
    onMessage: (m) => appendLiveMessage(m),
    onSessionUpdate: (detail) => {
      sessionDetail.value = detail;
    }
  });
}

function stopLiveChatWs() {
  if (liveWs) {
    liveWs.close();
    liveWs = null;
  }
}

const messages = ref<DisplayMsg[]>([]);
const loadingMessages = ref(false);
const scroller = ref<HTMLDivElement | null>(null);

async function loadMessages(sessionId: number) {
  loadingMessages.value = true;
  try {
    const data = await chatApi.sessionMessages(sessionId);
    messages.value = data.map(mapApiMessage);
    lastPollMessageId.value = data.reduce((m, x) => Math.max(m, x.id), 0);
    await loadSessionDetail(sessionId);
    if (isLiveMode.value) startLiveChatWs();
    else stopLiveChatWs();
    await scrollToBottom();
  } finally {
    loadingMessages.value = false;
  }
}

async function scrollToBottom(smooth = true) {
  await nextTick();
  if (scroller.value) {
    scroller.value.scrollTo({
      top: scroller.value.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto'
    });
  }
}

/** 流式 delta 时节流滚动，避免每个 token await 阻塞渲染 */
let scrollRaf = 0;
function scheduleScrollToBottom() {
  if (scrollRaf) return;
  scrollRaf = requestAnimationFrame(async () => {
    scrollRaf = 0;
    await scrollToBottom(false);
  });
}

/* —— 切换会话 —— */
async function selectSession(s: ChatSessionVO) {
  if (currentSessionId.value === s.id) return;
  stopLiveChatWs();
  currentSessionId.value = s.id;
  await loadMessages(s.id);
}

function newSession() {
  stopLiveChatWs();
  currentSessionId.value = null;
  sessionDetail.value = null;
  lastPollMessageId.value = 0;
  messages.value = [];
  input.value = '';
}

const handoffLoading = ref(false);

async function onRequestHandoff() {
  if (!currentSessionId.value || handoffLoading.value || isLiveMode.value) return;
  handoffLoading.value = true;
  try {
    const res = await liveChatApi.requestHandoff(currentSessionId.value);
    await loadSessionDetail(currentSessionId.value);
    startLiveChatWs();
    if (res.notice) {
      messages.value.push({
        id: res.systemMessageId ?? `sys-${Date.now()}`,
        role: 'SYSTEM',
        content: res.notice,
        createTime: new Date().toISOString()
      });
    }
    toast.success(res.ticketNo ? `已转人工，工单 ${res.ticketNo}` : '已转接人工客服');
    await scrollToBottom();
  } finally {
    handoffLoading.value = false;
  }
}

async function applyHandoffFromAnswer(handoffTriggered?: boolean) {
  if (!isUserRole.value || !handoffTriggered || !currentSessionId.value) return;
  await loadSessionDetail(currentSessionId.value);
  startLiveChatWs();
  const data = await chatApi.sessionMessages(currentSessionId.value);
  messages.value = data.map(mapApiMessage);
  lastPollMessageId.value = data.reduce((m, x) => Math.max(m, x.id), 0);
  await scrollToBottom(false);
}

function displayName(m: DisplayMsg) {
  if (m.role === 'USER') return '我';
  if (m.role === 'STAFF') return m.senderName || '人工客服';
  if (m.role === 'SYSTEM') return '系统';
  return '云屿 AI';
}

function msgClass(m: DisplayMsg) {
  if (m.role === 'USER') return 'msg--user';
  if (m.role === 'STAFF') return 'msg--staff';
  if (m.role === 'SYSTEM') return 'msg--system';
  return 'msg--ai';
}

/* —— 输入与发送 —— */
const input = ref('');
const inputRef = ref<HTMLTextAreaElement | null>(null);
const sending = ref(false);
const enableTools = ref(true); // 是否允许在检测到关键词时自动切到同步模式（工具调用）
const abortCtrl = ref<AbortController | null>(null);

const placeholder = computed(() => chatConfig.value.placeholder);

/** 结束「生成中」状态，恢复发送按钮与追问输入 */
function finishGenerating() {
  sending.value = false;
  abortCtrl.value = null;
  const last = messages.value[messages.value.length - 1];
  if (last?.role === 'ASSISTANT' && last.streaming) {
    last.streaming = false;
  }
}

async function onSend() {
  const question = input.value.trim();
  if (!question || sending.value) return;

  messages.value.push({
    id: `local-u-${Date.now()}`,
    role: 'USER',
    content: question,
    createTime: new Date().toISOString()
  });
  input.value = '';
  await scrollToBottom();

  if (isLiveMode.value && currentSessionId.value) {
    sending.value = true;
    const localUserId = messages.value[messages.value.length - 1]?.id;
    try {
      const msg = await liveChatApi.sendUserMessage(currentSessionId.value, {
        content: question
      });
      if (messages.value.some((x) => x.id === msg.id)) {
        const localIdx = messages.value.findIndex((x) => x.id === localUserId);
        if (localIdx >= 0) messages.value.splice(localIdx, 1);
      } else {
        const localIdx = messages.value.findIndex((x) => x.id === localUserId);
        if (localIdx >= 0) messages.value[localIdx] = mapApiMessage(msg);
        else appendLiveMessage(msg);
      }
      lastPollMessageId.value = Math.max(lastPollMessageId.value, msg.id);
      // 人工已接入时不再插入 AI 占位回复，客服消息由 WebSocket 推送
      if (serviceMode.value === 'WAITING_AGENT') {
        messages.value.push({
          id: `live-hint-${Date.now()}`,
          role: 'ASSISTANT',
          content: buildLiveModeHint(),
          createTime: new Date().toISOString()
        });
      }
    } finally {
      sending.value = false;
      await scrollToBottom();
    }
    return;
  }

  const useSync = enableTools.value && shouldUseSyncMode(question);
  sending.value = true;
  abortCtrl.value = new AbortController();

  // 3. 占位的 AI 气泡
  const ai: DisplayMsg = {
    id: `local-a-${Date.now()}`,
    role: 'ASSISTANT',
    content: '',
    streaming: true
  };
  messages.value.push(ai);
  await scrollToBottom();

  const payload: AskPayload = {
    sessionId: currentSessionId.value,
    questionText: question
  };

  try {
    if (useSync) {
      const res = await chatApi.ask(payload);
      currentSessionId.value = res.sessionId;
      ai.intent = res.intent;
      ai.modelName = res.modelName;
      ai.id = res.assistantMessageId;
      ai.sources = res.sources;
      ai.streaming = true;
      await revealTextProgressively(
        (chunk) => {
          ai.content = chunk;
        },
        res.answer,
        {
          signal: abortCtrl.value?.signal,
          onStep: scheduleScrollToBottom
        }
      );
      ai.streaming = false;
      finishGenerating();
      await applyHandoffFromAnswer(res.handoffTriggered);
      if (isUserRole.value && res.handoffTriggered) {
        toast.success(
          res.handoffTicketNo
            ? `已转人工客服，工单 ${res.handoffTicketNo}`
            : '已转接人工客服'
        );
      } else {
        toast.info(
          res.intent === 'TICKET_CREATE'
            ? '已为你创建工单 ✨ 可在「我的工单」查看'
            : '已结合工具调用为你回答'
        );
      }
      void loadSessions();
      await scrollToBottom();
    } else {
      await askStream(
        payload,
        {
          onMeta: (m) => {
            currentSessionId.value = m.sessionId;
            ai.sources = m.sources;
          },
          onDelta: (token) => {
            ai.content += token;
            scheduleScrollToBottom();
          },
          onDone: (d) => {
            ai.id = d.assistantMessageId;
            ai.intent = d.intent;
            ai.modelName = d.modelName;
            ai.streaming = false;
            ai.content = d.fullAnswer || ai.content;
            void applyHandoffFromAnswer(d.handoffTriggered);
            if (isUserRole.value && d.handoffTriggered) {
              toast.success(
                d.handoffTicketNo
                  ? `已转人工客服，工单 ${d.handoffTicketNo}`
                  : '已转接人工客服'
              );
            }
            finishGenerating();
            void loadSessions();
            void scrollToBottom();
          },
          onError: (e) => {
            ai.streaming = false;
            ai.error = true;
            ai.content = e.message || 'AI 服务繁忙，请稍后再试，或新建工单联系人工。';
            finishGenerating();
          }
        },
        abortCtrl.value.signal
      );
      finishGenerating();
    }
  } catch (e) {
    ai.streaming = false;
    ai.error = true;
    ai.content = '请求失败，请稍后重试。';
    finishGenerating();
  } finally {
    await scrollToBottom();
  }
}

function stopGenerating() {
  abortCtrl.value?.abort();
  const last = messages.value[messages.value.length - 1];
  if (last?.role === 'ASSISTANT') {
    last.streaming = false;
    if (!last.content) last.content = '（已停止）';
  }
  finishGenerating();
}

function onEnter(e: KeyboardEvent) {
  if (e.shiftKey) return;
  e.preventDefault();
  onSend();
}

/* —— 知识来源折叠 —— */
const expandedSources = ref<Set<number | string>>(new Set());
function toggleSources(id: number | string | undefined) {
  if (id == null) return;
  if (expandedSources.value.has(id)) expandedSources.value.delete(id);
  else expandedSources.value.add(id);
}

/* —— 快捷提问 —— */
const quickAsks = computed(() => chatConfig.value.quickAsks);

function quickAsk(text: string) {
  input.value = text;
  inputRef.value?.focus();
}

function buildLiveModeHint() {
  if (serviceMode.value === 'HUMAN') {
    const name = sessionDetail.value?.assignedStaffName;
    return name
      ? `客服 ${name} 正在接待您，请在本窗口查看回复。`
      : '人工客服已接入，请在本窗口查看回复。';
  }
  return '已转接人工客服，正在排队，请稍候。客服接入后将在此直接回复您。';
}

onMounted(async () => {
  await loadSessions();
  const sessionParam = route.query.session;
  if (sessionParam) {
    const sessionId = Number(sessionParam);
    if (!Number.isNaN(sessionId) && sessionId > 0) {
      const found = sessions.value.find((s) => s.id === sessionId);
      if (found) {
        await selectSession(found);
      } else {
        currentSessionId.value = sessionId;
        await loadMessages(sessionId);
      }
    }
  }
});

/* watch session change for highlight*/
watch(currentSessionId, (v) => {
  if (v == null) {
    messages.value = [];
    sessionDetail.value = null;
    stopLiveChatWs();
  }
});

onUnmounted(() => {
  stopLiveChatWs();
});
</script>

<template>
  <div class="chat-shell">
    <!-- 会话侧边栏 -->
    <aside class="chat-aside">
      <div class="chat-aside__top">
        <button class="btn btn--primary chat-aside__new" @click="newSession">
          <Icon name="plus" :size="14" /> 新建对话
        </button>
        <div class="chat-aside__search">
          <Icon name="search" :size="14" />
          <input v-model="searchSession" class="chat-aside__search-input" placeholder="搜索会话" />
        </div>
      </div>

      <div class="chat-aside__list">
        <button
          class="session-item session-item--draft"
          :class="{ active: currentSessionId === null }"
          @click="newSession"
        >
          <span class="session-item__dot">
            <Icon name="sparkle-dot" :size="14" />
          </span>
          <div class="session-item__body">
            <div class="session-item__title">开始新对话</div>
            <div class="session-item__sub">现在告诉 AI 你想聊什么</div>
          </div>
        </button>

        <button
          v-for="s in filteredSessions"
          :key="s.id"
          :class="['session-item', { active: currentSessionId === s.id }]"
          @click="selectSession(s)"
        >
          <span class="session-item__dot session-item__dot--soft">
            <Icon name="message" :size="14" />
          </span>
          <div class="session-item__body">
            <div class="session-item__title">{{ s.title || `会话 #${s.id}` }}</div>
            <div class="session-item__sub">
              {{ shorten(s.lastMessage, 28) || '（暂无消息）' }}
            </div>
          </div>
          <div class="session-item__time">{{ fromNow(s.updateTime || s.createTime) }}</div>
        </button>

        <div v-if="!loadingSessions && filteredSessions.length === 0" class="chat-aside__empty">
          {{ searchSession ? '没有匹配的会话' : '还没有会话，开始第一次提问吧' }}
        </div>
      </div>
    </aside>

    <!-- 主区域：消息流 -->
    <section class="chat-main">
      <div class="chat-main__head">
        <div class="chat-main__title">
          <span class="title-grad">{{ currentSessionId ? `会话 #${currentSessionId}` : chatConfig.title }}</span>
          <span class="chat-main__sub">{{ chatConfig.subtitle }} · deepseek-chat</span>
        </div>
        <div class="chat-main__head-extra">
          <div v-if="isLiveMode" class="live-banner">
            <Icon name="message" :size="14" />
            <span>{{ sessionDetail?.serviceModeName || '人工客服' }}</span>
            <span v-if="sessionDetail?.assignedStaffName">
              · {{ sessionDetail.assignedStaffName }}
            </span>
            <span v-if="sessionDetail?.activeTicketNo" class="live-banner__tk">
              {{ sessionDetail.activeTicketNo }}
            </span>
          </div>
          <button
            v-if="isUserRole && currentSessionId && !isLiveMode"
            class="btn btn--ghost btn--sm"
            :disabled="handoffLoading"
            @click="onRequestHandoff"
          >
            <Icon name="message" :size="14" /> 转人工
          </button>
          <label v-if="!isLiveMode" class="switch">
            <input type="checkbox" v-model="enableTools" />
            <span class="switch__track"><span class="switch__thumb"></span></span>
            <span class="switch__label">{{ chatConfig.toolsLabel }}</span>
            <span class="switch__hint" :title="chatConfig.toolsHint">?</span>
          </label>
        </div>
      </div>

      <div ref="scroller" class="chat-main__stream">
        <!-- 空状态：欢迎区 -->
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome__logo">
            <span class="ring ring--1"></span>
            <span class="ring ring--2"></span>
            <span class="ring ring--3"></span>
            <Avatar type="ai" :size="84">云</Avatar>
          </div>
          <h2 class="welcome__title">
            你好，{{ user.displayName }}
            <span class="wave">👋</span>
          </h2>
          <p class="welcome__sub" v-html="chatConfig.welcome.replace(/\n/g, '<br/>')" />
          <div class="welcome__quick">
            <button
              v-for="q in quickAsks"
              :key="q.text"
              class="welcome__chip"
              @click="quickAsk(q.text)"
            >
              <span class="welcome__chip-emoji">{{ q.icon }}</span> {{ q.text }}
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <div
            v-for="m in messages"
            :key="String(m.id)"
            :class="['msg', msgClass(m)]"
          >
            <div class="msg__avatar">
              <Avatar
                v-if="m.role === 'USER'"
                :name="user.displayName"
                :type="user.isAdmin ? 'admin' : user.isStaff ? 'staff' : 'user'"
                :size="36"
              />
              <Avatar
                v-else-if="m.role === 'STAFF'"
                type="staff"
                :name="m.senderName || '客服'"
                :size="36"
              />
              <Avatar v-else-if="m.role === 'SYSTEM'" type="ai" :size="36">!</Avatar>
              <Avatar v-else type="ai" :size="36">云</Avatar>
            </div>
            <div class="msg__body">
              <div class="msg__meta">
                <span>{{ displayName(m) }}</span>
                <span v-if="m.modelName" class="msg__meta-tag">{{ m.modelName }}</span>
                <span v-if="m.intent && m.intent !== 'GENERAL_CHAT'" class="msg__meta-tag msg__meta-tag--accent">
                  · 意图：{{ m.intent }}
                </span>
                <span v-if="m.createTime" class="msg__meta-time">{{ formatTime(m.createTime, 'HH:mm:ss') }}</span>
              </div>

              <div
                :class="[
                  'bubble',
                  m.role === 'USER'
                    ? 'bubble--user'
                    : m.role === 'STAFF'
                      ? 'bubble--staff'
                      : m.role === 'SYSTEM'
                        ? 'bubble--system'
                        : 'bubble--ai',
                  m.error ? 'bubble--error' : ''
                ]"
              >
                <div v-if="m.role === 'USER'" class="bubble__user-text">{{ m.content }}</div>
                <div
                  v-else-if="m.role === 'STAFF' || m.role === 'SYSTEM'"
                  class="bubble__plain"
                >{{ m.content }}</div>
                <div v-else class="markdown" v-html="renderMarkdown(m.content)"></div>
                <span v-if="m.streaming" class="cursor-blink"></span>
              </div>

              <!-- 知识来源（仅 AI 回复，且有来源）-->
              <div
                v-if="m.role === 'ASSISTANT' && m.sources && m.sources.length > 0"
                class="sources"
              >
                <button class="sources__toggle" @click="toggleSources(m.id)">
                  <Icon name="book" :size="14" />
                  <span>引用了 {{ m.sources.length }} 条知识</span>
                  <Icon :name="expandedSources.has(m.id!) ? 'chevron-down' : 'chevron-right'" :size="12" />
                </button>
                <div v-if="expandedSources.has(m.id!)" class="sources__list">
                  <div v-for="(src, i) in m.sources" :key="i" class="source-card">
                    <div class="source-card__head">
                      <span class="chip chip--iris">#{{ i + 1 }}</span>
                      <span v-if="src.documentName" class="source-card__doc">
                        <Icon name="file" :size="12" />
                        {{ src.documentName }}
                      </span>
                      <span v-if="src.score != null" class="source-card__score">
                        相似度 {{ (src.score * 100).toFixed(1) }}%
                      </span>
                    </div>
                    <div class="source-card__body">{{ src.chunkContentPreview }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 输入区 -->
      <footer class="composer">
        <div class="composer__inner">
          <textarea
            ref="inputRef"
            v-model="input"
            class="composer__input"
            :placeholder="placeholder"
            rows="1"
            @keydown.enter="onEnter"
          />
          <div class="composer__actions">
            <div class="composer__hint">
              <Icon name="lightbulb" :size="13" />
              <span>
                {{ isLiveMode
                  ? '人工客服模式 · 消息将直接发送给在线客服'
                  : enableTools && shouldUseSyncMode(input)
                    ? '将调用业务工具（同步回答，打字机展示）'
                    : '流式输出 · 回车发送，Shift + 回车换行' }}
              </span>
            </div>
            <button
              v-if="sending"
              class="btn btn--ghost composer__send"
              type="button"
              @click="stopGenerating"
            >
              <Icon name="close" :size="14" /> 停止生成
            </button>
            <button
              v-else
              class="btn btn--primary composer__send"
              :disabled="!input.trim()"
              @click="onSend"
            >
              <Icon name="send" :size="14" /> 发送
            </button>
          </div>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.chat-shell {
  display: grid;
  grid-template-columns: 280px 1fr;
  height: calc(100vh - var(--layout-header-h) - 26px - 28px);
  max-height: calc(100vh - var(--layout-header-h) - 26px - 28px);
  min-height: 560px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(14px) saturate(140%);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-md);
}

/* —— 侧边会话栏 —— */
.chat-aside {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  border-right: 1px solid var(--border-soft);
  background: rgba(255, 255, 255, 0.6);
}
.chat-aside__top {
  padding: 14px 14px 8px;
  display: flex; flex-direction: column; gap: 10px;
}
.chat-aside__new {
  height: 38px;
  width: 100%;
  font-weight: 600;
}
.chat-aside__search {
  display: flex; align-items: center; gap: 8px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  padding: 0 12px;
  height: 36px;
  color: var(--color-ink-400);
}
.chat-aside__search-input {
  flex: 1; border: none; outline: none;
  background: transparent;
  font-size: var(--fs-sm);
  color: var(--color-ink-900);
}

.chat-aside__list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px 16px;
}

.session-item {
  width: 100%;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 10px;
  margin-bottom: 4px;
  border-radius: var(--radius-md);
  background: transparent;
  border: 1px solid transparent;
  cursor: pointer;
  text-align: left;
  transition: all var(--transition-fast);
  position: relative;
}
.session-item:hover {
  background: rgba(169, 215, 242, 0.18);
}
.session-item.active {
  background: linear-gradient(135deg, rgba(169, 215, 242, 0.35), rgba(214, 204, 245, 0.35));
  border-color: rgba(120, 160, 210, 0.22);
}
.session-item--draft .session-item__sub { color: var(--color-primary-600); }

.session-item__dot {
  width: 30px; height: 30px;
  border-radius: 10px;
  display: inline-flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #A9D7F2, #9B8AD4);
  color: #fff;
  flex-shrink: 0;
}
.session-item__dot--soft {
  background: linear-gradient(135deg, #DCEEFB, #E4D8FD);
  color: var(--color-primary-600);
}
.session-item__body {
  flex: 1; min-width: 0;
  line-height: 1.4;
}
.session-item__title {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--color-ink-900);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-item__sub {
  font-size: var(--fs-xs);
  color: var(--color-ink-500);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-item__time {
  font-size: 10px;
  color: var(--color-ink-400);
  position: absolute;
  top: 10px;
  right: 10px;
}

.chat-aside__empty {
  text-align: center;
  color: var(--color-ink-400);
  font-size: var(--fs-sm);
  padding: 32px 14px;
}

/* —— 主区域 —— */
.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
.chat-main__head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 22px;
  border-bottom: 1px solid var(--border-soft);
  background: rgba(255,255,255,0.55);
}
.chat-main__head-extra {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.live-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  background: rgba(189, 234, 218, 0.45);
  font-size: var(--fs-sm);
  color: var(--color-ink-700);
}
.live-banner__tk { font-size: 11px; opacity: 0.85; }
.btn--sm { height: 32px; padding: 0 12px; font-size: var(--fs-sm); }
.msg--staff { flex-direction: row; }
.bubble--staff { background: #e8f8f0; border-color: #bdeada; }
.bubble--system { background: #fff8e8; border-color: #f0e0b8; }
.bubble__plain { white-space: pre-wrap; line-height: 1.55; }
.chat-main__title {
  display: flex; flex-direction: column;
  line-height: 1.2;
}
.title-grad {
  font-size: var(--fs-lg);
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-primary-600), #9B8AD4);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.chat-main__sub {
  font-size: var(--fs-xs);
  color: var(--color-ink-400);
  margin-top: 4px;
}

/* 开关 */
.switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}
.switch input { display: none; }
.switch__track {
  position: relative;
  width: 36px; height: 20px;
  background: var(--color-ink-200);
  border-radius: 999px;
  transition: background var(--transition-fast);
}
.switch__thumb {
  position: absolute;
  top: 2px; left: 2px;
  width: 16px; height: 16px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  transition: all var(--transition-fast);
}
.switch input:checked + .switch__track {
  background: linear-gradient(135deg, var(--color-primary-400), var(--color-primary-600));
}
.switch input:checked + .switch__track .switch__thumb {
  left: 18px;
}
.switch__label {
  font-size: var(--fs-sm);
  color: var(--color-ink-700);
  font-weight: 500;
}
.switch__hint {
  width: 16px; height: 16px;
  border-radius: 50%;
  background: var(--color-ink-100);
  color: var(--color-ink-500);
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 10px;
  cursor: help;
}

.chat-main__stream {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px 8% 16px;
  overscroll-behavior: contain;
}

/* —— 欢迎区 —— */
.welcome {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px;
}
.welcome__logo {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}
.ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(120, 160, 210, 0.25);
  animation: pulse 3s ease-in-out infinite;
}
.ring--1 { width: 110px; height: 110px; animation-delay: 0s; }
.ring--2 { width: 140px; height: 140px; animation-delay: 1s; opacity: 0.6; }
.ring--3 { width: 170px; height: 170px; animation-delay: 2s; opacity: 0.3; }
@keyframes pulse {
  0%   { transform: scale(0.9); opacity: 0; }
  40%  { opacity: 1; }
  100% { transform: scale(1.15); opacity: 0; }
}

.welcome__title {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-ink-900);
  margin: 0;
}
.wave { display: inline-block; animation: wave 1.6s ease-in-out infinite; transform-origin: 70% 70%; }
@keyframes wave {
  0%, 60%, 100% { transform: rotate(0); }
  10% { transform: rotate(14deg); }
  20% { transform: rotate(-8deg); }
  30% { transform: rotate(14deg); }
  40% { transform: rotate(-4deg); }
  50% { transform: rotate(10deg); }
}
.welcome__sub {
  color: var(--color-ink-500);
  font-size: var(--fs-md);
  line-height: var(--lh-loose);
  margin-top: 10px;
}
.welcome__quick {
  display: flex; flex-wrap: wrap; gap: 10px; justify-content: center;
  margin-top: 24px;
}
.welcome__chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: 999px;
  font-size: var(--fs-sm);
  color: var(--color-ink-700);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: 0 4px 12px rgba(80, 120, 180, 0.06);
}
.welcome__chip:hover {
  border-color: var(--color-primary-300);
  color: var(--color-primary-700);
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(80, 120, 180, 0.12);
}
.welcome__chip-emoji { font-size: 14px; }

/* —— 消息气泡 —— */
.msg {
  display: flex;
  gap: 12px;
  margin-bottom: 22px;
  align-items: flex-start;
}
.msg--user {
  flex-direction: row-reverse;
}
.msg__body {
  max-width: 78%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.msg--user .msg__body { align-items: flex-end; }
.msg--ai   .msg__body { align-items: flex-start; }

.msg__meta {
  display: flex; align-items: center; gap: 6px;
  font-size: var(--fs-xs);
  color: var(--color-ink-500);
  margin-bottom: 6px;
}
.msg__meta-tag {
  background: var(--color-ink-100);
  border-radius: 999px;
  padding: 1px 8px;
  color: var(--color-ink-500);
}
.msg__meta-tag--accent {
  background: var(--bg-tint-iris);
  color: #6D5DB6;
}
.msg__meta-time { color: var(--color-ink-400); margin-left: 4px; }

.bubble {
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  font-size: var(--fs-md);
  line-height: var(--lh-base);
  position: relative;
  word-break: break-word;
  box-shadow: var(--shadow-sm);
}
.bubble--user {
  background: linear-gradient(135deg, var(--color-primary-400), var(--color-primary-600));
  color: #fff;
  border-bottom-right-radius: 8px;
}
.bubble--user .bubble__user-text {
  white-space: pre-wrap;
}
.bubble--ai {
  background: #fff;
  border: 1px solid var(--border-card);
  color: var(--color-ink-900);
  border-bottom-left-radius: 8px;
}
.bubble--ai::before {
  content: '';
  position: absolute;
  left: -6px;
  top: 16px;
  width: 12px; height: 12px;
  background: #fff;
  border-left: 1px solid var(--border-card);
  border-bottom: 1px solid var(--border-card);
  transform: rotate(45deg);
  border-bottom-left-radius: 2px;
}
.bubble--error {
  background: #FFF4F4;
  border-color: #F4C6CA;
  color: #B14855;
}

/* —— 知识来源 —— */
.sources {
  margin-top: 8px;
  width: 100%;
  max-width: 100%;
}
.sources__toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--bg-tint-iris);
  color: #6D5DB6;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: var(--fs-xs);
  cursor: pointer;
  border: none;
}
.sources__toggle:hover { filter: brightness(0.96); }
.sources__list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.source-card {
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: var(--fs-xs);
  color: var(--color-ink-700);
}
.source-card__head {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 6px;
}
.source-card__doc {
  display: inline-flex; align-items: center; gap: 4px;
  color: var(--color-ink-500);
}
.source-card__score {
  margin-left: auto;
  color: var(--color-primary-600);
  font-weight: 500;
}
.source-card__body {
  color: var(--color-ink-700);
  line-height: var(--lh-base);
  white-space: pre-wrap;
}

/* —— 输入区 —— */
.composer {
  flex-shrink: 0;
  border-top: 1px solid var(--border-soft);
  padding: 16px 22px 18px;
  background: rgba(255,255,255,0.7);
}
.composer__inner {
  background: #fff;
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  padding: 14px 16px 10px;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-sm);
}
.composer__inner:focus-within {
  border-color: var(--color-primary-300);
  box-shadow: 0 0 0 4px rgba(106, 168, 230, 0.14);
}
.composer__input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  font-size: var(--fs-md);
  font-family: inherit;
  color: var(--color-ink-900);
  background: transparent;
  min-height: 24px;
  max-height: 200px;
  line-height: var(--lh-base);
}
.composer__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  gap: 12px;
}
.composer__hint {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: var(--fs-xs);
  color: var(--color-ink-400);
}
.composer__send {
  height: 36px;
  padding: 0 18px;
  font-weight: 600;
}

@media (max-width: 880px) {
  .chat-shell {
    grid-template-columns: 1fr;
  }
  .chat-aside { display: none; }
  .chat-main__stream { padding: 18px 16px 12px; }
  .msg__body { max-width: 88%; }
}
</style>
