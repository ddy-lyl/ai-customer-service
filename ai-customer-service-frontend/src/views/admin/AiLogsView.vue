<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { aiLogsApi } from '@/api/admin';
import type {
  AiTraceVO,
  ChatSessionVO,
  PageResult
} from '@/api/types';
import { formatTime, fromNow, shorten } from '@/utils/format';
import { renderMarkdown } from '@/utils/markdown';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import EmptyState from '@/components/EmptyState.vue';
import Pagination from '@/components/Pagination.vue';
import Avatar from '@/components/Avatar.vue';
import Icon from '@/components/Icon.vue';

const route = useRoute();
const router = useRouter();

/* —— 会话列表 —— */
const sessions = ref<PageResult<ChatSessionVO>>({
  pageNo: 1, pageSize: 10, total: 0, pages: 1, records: []
});
const sessionsLoading = ref(false);
const sessionsPage = ref(1);

async function loadSessions() {
  sessionsLoading.value = true;
  try {
    sessions.value = await aiLogsApi.sessionsPage({
      pageNo: sessionsPage.value,
      pageSize: 12
    });
  } finally { sessionsLoading.value = false; }
}
watch(sessionsPage, loadSessions);

/* —— Trace —— */
const trace = ref<AiTraceVO | null>(null);
const traceLoading = ref(false);
const currentSessionId = ref<number | null>(null);

async function loadTrace(id: number) {
  currentSessionId.value = id;
  traceLoading.value = true;
  trace.value = null;
  try {
    trace.value = await aiLogsApi.trace(id);
  } finally { traceLoading.value = false; }
}

/* 构造时间线：消息 + 工具调用 + 召回穿插展示 */
interface TraceItem {
  kind: 'message' | 'retrieval' | 'tool';
  time: string;
  data: any;
}
const traceTimeline = computed<TraceItem[]>(() => {
  if (!trace.value) return [];
  const arr: TraceItem[] = [];
  trace.value.messages
    .filter((m) => m.role === 'USER' || m.role === 'ASSISTANT')
    .forEach((m) => arr.push({ kind: 'message', time: m.createTime, data: m }));
  trace.value.retrievalLogs?.forEach((r) =>
    arr.push({ kind: 'retrieval', time: r.createTime, data: r })
  );
  trace.value.toolCallLogs?.forEach((t) =>
    arr.push({ kind: 'tool', time: t.createTime, data: t })
  );
  arr.sort((a, b) => (a.time < b.time ? -1 : 1));
  return arr;
});

const traceStats = computed(() => {
  if (!trace.value) return null;
  const msgs = trace.value.messages.filter((m) => m.role === 'USER' || m.role === 'ASSISTANT');
  return {
    messageCount: msgs.length,
    retrievalCount: trace.value.retrievalLogs?.length ?? 0,
    toolCallCount: trace.value.toolCallLogs?.length ?? 0,
    totalCost:
      trace.value.toolCallLogs?.reduce((s, t) => s + (t.costMillis || 0), 0) ?? 0
  };
});

function safeJson(s: string | undefined | null) {
  if (!s) return '';
  try {
    return JSON.stringify(JSON.parse(s), null, 2);
  } catch { return s; }
}

onMounted(async () => {
  await loadSessions();
  const sId = route.query.session ? Number(route.query.session) : null;
  if (sId) await loadTrace(sId);
  else if (sessions.value.records[0]) await loadTrace(sessions.value.records[0].id);
});
</script>

<template>
  <div>
    <PageHeader
      title="AI 链路追踪"
      subtitle="把每一次对话拆解成「消息 → 召回切片 → AI 回答 → 工具调用」全链路审计"
    />

    <div class="trace-grid">
      <!-- 左：会话列表 -->
      <aside class="card trace-aside">
        <div class="trace-aside__head">
          <span>AI 会话</span>
          <span class="text-faint" style="font-size: var(--fs-xs)">共 {{ sessions.total }} 个</span>
        </div>
        <div v-if="sessionsLoading" class="loading-row" style="padding: 24px 0">
          <div class="spinner"></div>
        </div>
        <EmptyState v-else-if="sessions.records.length === 0" title="还没有 AI 会话" icon="message" />
        <ul v-else class="ses-list">
          <li
            v-for="s in sessions.records"
            :key="s.id"
            :class="['ses-item', { active: currentSessionId === s.id }]"
            @click="loadTrace(s.id)"
          >
            <div class="ses-item__dot"><Icon name="sparkle-dot" :size="13" /></div>
            <div class="ses-item__body">
              <div class="ses-item__title">{{ s.title || `会话 #${s.id}` }}</div>
              <div class="ses-item__sub">{{ shorten(s.lastMessage, 30) || '—' }}</div>
              <div class="ses-item__time">{{ fromNow(s.updateTime || s.createTime) }}</div>
            </div>
          </li>
        </ul>
        <Pagination
          v-if="sessions.total > 0"
          :page-no="sessions.pageNo"
          :page-size="sessions.pageSize"
          :total="sessions.total"
          @change="(p) => (sessionsPage = p)"
        />
      </aside>

      <!-- 右：Trace 详情 -->
      <section class="card trace-main">
        <div v-if="traceLoading" class="loading-row" style="padding: 80px 0">
          <div class="spinner"></div>
        </div>

        <EmptyState
          v-else-if="!trace"
          title="请选择左侧的会话查看链路"
          icon="logs"
        />

        <template v-else>
          <div class="trace-main__head">
            <div>
              <div class="trace-main__title">
                <span class="title-grad">会话 #{{ trace.session?.id }}</span>
              </div>
              <div class="trace-main__sub">
                {{ traceStats?.messageCount }} 条消息 · 召回 {{ traceStats?.retrievalCount }} 次 ·
                工具调用 {{ traceStats?.toolCallCount }} 次 · 累计耗时 {{ traceStats?.totalCost }} ms
              </div>
            </div>
          </div>

          <!-- 时间线 -->
          <ol class="ttl">
            <li v-for="(item, i) in traceTimeline" :key="i" class="ttl__item">
              <!-- 用户/AI 消息 -->
              <template v-if="item.kind === 'message'">
                <div class="ttl__dot ttl__dot--msg">
                  <Avatar
                    v-if="item.data.role === 'USER'"
                    :name="String(item.data.userId)"
                    :size="28"
                  />
                  <Avatar v-else type="ai" :size="28">云</Avatar>
                </div>
                <div class="ttl__body">
                  <div class="ttl__head">
                    <span class="ttl__title">
                      {{ item.data.role === 'USER' ? '用户提问' : 'AI 回答' }}
                    </span>
                    <span v-if="item.data.modelName" class="ttl__tag">{{ item.data.modelName }}</span>
                    <span v-if="item.data.intent && item.data.intent !== 'GENERAL_CHAT'" class="ttl__tag ttl__tag--iris">
                      意图：{{ item.data.intent }}
                    </span>
                    <span class="ttl__time">{{ formatTime(item.data.createTime, 'HH:mm:ss') }}</span>
                  </div>
                  <div :class="['ttl__bubble', `ttl__bubble--${item.data.role.toLowerCase()}`]">
                    <div v-if="item.data.role === 'USER'" style="white-space: pre-wrap">{{ item.data.content }}</div>
                    <div v-else class="markdown" v-html="renderMarkdown(item.data.content)"></div>
                  </div>
                </div>
              </template>

              <!-- 召回 -->
              <template v-else-if="item.kind === 'retrieval'">
                <div class="ttl__dot ttl__dot--retrieval">
                  <Icon name="doc-search" :size="14" />
                </div>
                <div class="ttl__body">
                  <div class="ttl__head">
                    <span class="ttl__title">RAG 知识库召回</span>
                    <span v-if="item.data.rankNo != null" class="ttl__tag">#{{ item.data.rankNo }}</span>
                    <span v-if="item.data.documentName" class="ttl__tag">{{ item.data.documentName }}</span>
                    <span v-if="item.data.score != null" class="ttl__tag ttl__tag--success">
                      相似度 {{ (item.data.score * 100).toFixed(1) }}%
                    </span>
                    <span class="ttl__time">{{ formatTime(item.data.createTime, 'HH:mm:ss') }}</span>
                  </div>
                  <div class="ttl__sub">
                    <span class="text-faint">问题：</span>{{ item.data.questionText || item.data.questionPreview }}
                  </div>
                  <div v-if="item.data.chunkContentPreview" class="ttl__sub" style="margin-top: 6px">
                    {{ shorten(item.data.chunkContentPreview, 200) }}
                  </div>
                </div>
              </template>

              <!-- 工具调用 -->
              <template v-else-if="item.kind === 'tool'">
                <div class="ttl__dot ttl__dot--tool">
                  <Icon name="tool" :size="14" />
                </div>
                <div class="ttl__body">
                  <div class="ttl__head">
                    <span class="ttl__title">工具调用 · {{ item.data.toolName }}</span>
                    <StatusChip :tone="item.data.status === 'SUCCESS' ? 'success' : 'danger'" dot>
                      {{ item.data.status }}
                    </StatusChip>
                    <span class="ttl__tag">{{ item.data.costMillis }}ms</span>
                    <span class="ttl__time">{{ formatTime(item.data.createTime, 'HH:mm:ss') }}</span>
                  </div>
                  <div class="ttl__call">
                    <div class="ttl__call-block">
                      <div class="ttl__call-lbl">入参</div>
                      <pre class="ttl__code">{{ safeJson(item.data.requestJson) }}</pre>
                    </div>
                    <div class="ttl__call-block">
                      <div class="ttl__call-lbl">结果</div>
                      <pre class="ttl__code">{{ safeJson(item.data.responseJson) }}</pre>
                    </div>
                  </div>
                </div>
              </template>
            </li>
          </ol>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.trace-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
@media (max-width: 1080px) {
  .trace-grid { grid-template-columns: 1fr; }
}

.trace-aside {
  padding: 14px;
  max-height: calc(100vh - var(--layout-header-h) - 110px);
  display: flex; flex-direction: column;
}
.trace-aside__head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 4px 8px 10px;
  font-weight: 600;
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
  border-bottom: 1px solid var(--border-soft);
  margin-bottom: 8px;
}
.ses-list {
  flex: 1;
  overflow-y: auto;
  margin: 0 -4px;
}
.ses-item {
  display: flex; gap: 10px;
  padding: 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}
.ses-item:hover { background: var(--color-primary-50); }
.ses-item.active {
  background: linear-gradient(135deg, rgba(169,215,242,0.35), rgba(214,204,245,0.35));
}
.ses-item__dot {
  width: 30px; height: 30px;
  border-radius: 10px;
  background: linear-gradient(135deg, #DCEEFB, #E4D8FD);
  color: var(--color-primary-600);
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ses-item__title {
  font-size: var(--fs-sm);
  font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ses-item__sub {
  font-size: var(--fs-xs);
  color: var(--color-ink-500);
  margin-top: 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ses-item__time {
  font-size: 10px;
  color: var(--color-ink-400);
  margin-top: 4px;
}

.trace-main {
  padding: 18px 22px;
  min-height: 600px;
}
.trace-main__head {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-soft);
}
.trace-main__title {
  font-size: var(--fs-xl);
  font-weight: 700;
}
.title-grad {
  background: linear-gradient(135deg, var(--color-primary-600), #9B8AD4);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.trace-main__sub {
  margin-top: 4px;
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
}

/* —— 时间线 —— */
.ttl {
  position: relative;
  padding-left: 4px;
}
.ttl::before {
  content: '';
  position: absolute;
  left: 14px;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: var(--border-soft);
}
.ttl__item {
  position: relative;
  padding: 0 0 18px 44px;
}
.ttl__dot {
  position: absolute;
  left: 0; top: 2px;
  width: 30px; height: 30px;
  border-radius: 10px;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff;
  z-index: 1;
  box-shadow: 0 4px 10px rgba(80, 120, 180, 0.18);
}
.ttl__dot--msg { background: transparent; box-shadow: none; padding: 0; width: 30px; height: 30px; }
.ttl__dot--retrieval { background: linear-gradient(135deg, #9CDDC1, #5FB89B); }
.ttl__dot--tool      { background: linear-gradient(135deg, #C0B0F0, #8E78D3); }

.ttl__head {
  display: flex; align-items: center; gap: 8px;
  flex-wrap: wrap;
}
.ttl__title {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--color-ink-900);
}
.ttl__tag {
  background: var(--color-ink-100);
  color: var(--color-ink-500);
  font-size: var(--fs-xs);
  padding: 1px 8px;
  border-radius: 999px;
}
.ttl__tag--iris { background: var(--bg-tint-iris); color: #6D5DB6; }
.ttl__tag--success { background: #E1F4EC; color: #2E8C6E; }
.ttl__time {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-ink-400);
  font-family: var(--font-mono);
}

.ttl__bubble {
  margin-top: 8px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: var(--fs-sm);
  line-height: var(--lh-base);
  border: 1px solid var(--border-card);
  background: #fff;
}
.ttl__bubble--user {
  background: linear-gradient(135deg, rgba(169,215,242,0.18), rgba(214,204,245,0.10));
  border-color: rgba(120, 160, 210, 0.18);
}

.ttl__sub {
  margin-top: 6px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  background: var(--bg-tint-mint);
  color: #2E8C6E;
  font-size: var(--fs-sm);
}

.ttl__call {
  margin-top: 8px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
@media (max-width: 720px) {
  .ttl__call { grid-template-columns: 1fr; }
}
.ttl__call-block {
  display: flex; flex-direction: column;
}
.ttl__call-lbl {
  font-size: var(--fs-xs);
  color: var(--color-ink-500);
  margin-bottom: 4px;
}
.ttl__code {
  background: #0E1B2E;
  color: #E6EEF8;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.55;
  margin: 0;
  overflow: auto;
  max-height: 200px;
  white-space: pre-wrap;
  word-break: break-word;
}

.loading-row { display: flex; align-items: center; justify-content: center; gap: 10px; }
</style>
