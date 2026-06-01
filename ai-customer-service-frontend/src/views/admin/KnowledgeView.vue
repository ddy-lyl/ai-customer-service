<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { knowledgeApi } from '@/api/admin';
import type { KnowledgeBaseVO, KnowledgeDocumentVO, PageResult } from '@/api/types';
import { formatFileSize, formatTime, fromNow } from '@/utils/format';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';
import PageHeader from '@/components/PageHeader.vue';
import StatusChip from '@/components/StatusChip.vue';
import Modal from '@/components/Modal.vue';
import Pagination from '@/components/Pagination.vue';
import EmptyState from '@/components/EmptyState.vue';
import Icon from '@/components/Icon.vue';

const toast = useToast();
const { open: confirm } = useConfirm();

/* —— 知识库列表 —— */
const bases = ref<KnowledgeBaseVO[]>([]);
const activeBase = ref<KnowledgeBaseVO | null>(null);
const basesLoading = ref(false);

async function loadBases() {
  basesLoading.value = true;
  try {
    const data = await knowledgeApi.basePage({ pageNo: 1, pageSize: 100 });
    bases.value = data.records;
    if (!activeBase.value && bases.value.length > 0) {
      activeBase.value = bases.value[0];
    } else if (activeBase.value) {
      activeBase.value = bases.value.find((b) => b.id === activeBase.value!.id) ?? null;
    }
  } finally { basesLoading.value = false; }
}

const baseFormOpen = ref(false);
const baseEditing = ref<KnowledgeBaseVO | null>(null);
const baseForm = reactive({ name: '', description: '' });

function openCreateBase() {
  baseEditing.value = null;
  Object.assign(baseForm, { name: '', description: '' });
  baseFormOpen.value = true;
}
function openEditBase(b: KnowledgeBaseVO) {
  baseEditing.value = b;
  Object.assign(baseForm, { name: b.name, description: b.description ?? '' });
  baseFormOpen.value = true;
}

async function submitBase() {
  if (!baseForm.name.trim()) return toast.warning('请输入名称');
  if (baseEditing.value) {
    await knowledgeApi.updateBase(baseEditing.value.id, {
      name: baseForm.name.trim(),
      description: baseForm.description.trim()
    });
    toast.success('已更新');
  } else {
    await knowledgeApi.createBase({
      name: baseForm.name.trim(),
      description: baseForm.description.trim() || undefined
    });
    toast.success('已创建');
  }
  baseFormOpen.value = false;
  await loadBases();
}

async function toggleBaseStatus(b: KnowledgeBaseVO) {
  const next = b.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  await knowledgeApi.toggleStatus(b.id, { status: next });
  toast.success(next === 'ENABLED' ? '已启用' : '已禁用');
  await loadBases();
}

async function deleteBase(b: KnowledgeBaseVO) {
  const ok = await confirm({
    title: '删除知识库',
    message: `确认删除 “${b.name}” 吗？关联文档将不可用，请谨慎操作。`,
    tone: 'danger'
  });
  if (!ok) return;
  await knowledgeApi.deleteBase(b.id);
  toast.success('已删除');
  activeBase.value = null;
  await loadBases();
}

/* —— 文档列表 —— */
const docs = ref<PageResult<KnowledgeDocumentVO>>({
  pageNo: 1, pageSize: 10, total: 0, pages: 1, records: []
});
const docsLoading = ref(false);
const docsPage = ref(1);

async function loadDocs() {
  if (!activeBase.value) {
    docs.value = { pageNo: 1, pageSize: 10, total: 0, pages: 1, records: [] };
    return;
  }
  docsLoading.value = true;
  try {
    docs.value = await knowledgeApi.documentPage({
      pageNo: docsPage.value,
      pageSize: 10,
      knowledgeBaseId: activeBase.value.id
    });
  } finally { docsLoading.value = false; }
}

watch([activeBase, docsPage], () => { docsPage.value = 1; loadDocs(); });

/* —— 上传 —— */
const fileInput = ref<HTMLInputElement | null>(null);
const uploading = ref(false);
function pickFile() { fileInput.value?.click(); }
async function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement;
  const file = target.files?.[0];
  target.value = '';
  if (!file || !activeBase.value) return;
  uploading.value = true;
  try {
    await knowledgeApi.upload(activeBase.value.id, file);
    toast.success(`已上传 ${file.name}`);
    await loadDocs();
  } finally { uploading.value = false; }
}

/* —— 单文档操作 —— */
async function parse(d: KnowledgeDocumentVO) {
  await knowledgeApi.parseOne(d.id);
  toast.success('已触发解析，请稍后刷新');
  setTimeout(loadDocs, 1500);
}
async function chunk(d: KnowledgeDocumentVO) {
  await knowledgeApi.chunk(d.id);
  toast.success('已触发切片');
  setTimeout(loadDocs, 1500);
}
async function vectorize(d: KnowledgeDocumentVO) {
  await knowledgeApi.vectorize(d.id);
  toast.success('已触发向量化');
  setTimeout(loadDocs, 1500);
}

async function deleteDoc(d: KnowledgeDocumentVO) {
  const ok = await confirm({
    title: '删除文档',
    message: `确认删除 “${d.originalFilename}” 吗？将同时删除切片与本地文件，已向量化的内容也会从检索中移除。`,
    tone: 'danger'
  });
  if (!ok) return;
  await knowledgeApi.deleteDocument(d.id);
  toast.success('文档已删除');
  await loadDocs();
}

async function parseAll() {
  if (!activeBase.value) return;
  await knowledgeApi.parseAll(activeBase.value.id);
  toast.success('已批量解析');
  setTimeout(loadDocs, 1500);
}

onMounted(async () => {
  await loadBases();
  await loadDocs();
});
</script>

<template>
  <div>
    <PageHeader
      title="知识库管理"
      subtitle="上传 → 解析 → 切片 → 向量化 · 让 AI 拥有你的业务知识"
    >
      <template #extra>
        <button class="btn btn--primary" @click="openCreateBase">
          <Icon name="plus" :size="14" /> 新建知识库
        </button>
      </template>
    </PageHeader>

    <div class="kb-grid">
      <!-- 左：知识库列表 -->
      <aside class="kb-aside card">
        <div class="kb-aside__head">
          <span>知识库</span>
          <span class="text-faint" style="font-size: var(--fs-xs)">{{ bases.length }} 个</span>
        </div>
        <div v-if="basesLoading" class="loading-row" style="padding: 24px 0">
          <div class="spinner"></div>
        </div>
        <EmptyState
          v-else-if="bases.length === 0"
          title="还没有知识库"
          description="点击右上角创建第一个"
          icon="book"
        />
        <ul v-else class="kb-list">
          <li
            v-for="b in bases"
            :key="b.id"
            :class="['kb-item', { active: activeBase?.id === b.id }]"
            @click="activeBase = b"
          >
            <div class="kb-item__icon">
              <Icon name="book" :size="16" />
            </div>
            <div class="kb-item__body">
              <div class="kb-item__name">{{ b.name }}</div>
              <div class="kb-item__sub">
                <span class="text-faint">ID {{ b.id }}</span>
                <StatusChip :tone="b.status === 'ENABLED' ? 'success' : 'info'">
                  {{ b.status === 'ENABLED' ? '启用' : '停用' }}
                </StatusChip>
              </div>
            </div>
          </li>
        </ul>
      </aside>

      <!-- 右：文档列表 -->
      <section class="kb-main card">
        <div v-if="!activeBase" class="empty-state">
          <EmptyState title="请选择一个知识库" icon="book" />
        </div>
        <template v-else>
          <div class="kb-main__head">
            <div>
              <div class="kb-main__title">{{ activeBase.name }}</div>
              <div class="kb-main__sub">
                <span v-if="activeBase.description" class="text-muted">{{ activeBase.description }}</span>
                <span v-else class="text-faint">暂无描述</span>
              </div>
            </div>
            <div class="kb-main__actions">
              <button class="btn btn--ghost btn--sm" @click="openEditBase(activeBase)">
                编辑
              </button>
              <button class="btn btn--ghost btn--sm" @click="toggleBaseStatus(activeBase)">
                {{ activeBase.status === 'ENABLED' ? '停用' : '启用' }}
              </button>
              <button class="btn btn--danger btn--sm" @click="deleteBase(activeBase)">删除</button>
            </div>
          </div>

          <div class="kb-main__toolbar">
            <button class="btn btn--primary btn--sm" :disabled="uploading" @click="pickFile">
              <Icon name="upload" :size="13" />
              {{ uploading ? '上传中…' : '上传文档' }}
            </button>
            <button class="btn btn--ghost btn--sm" @click="parseAll">
              <Icon name="refresh" :size="13" /> 批量解析
            </button>
            <button class="btn btn--ghost btn--sm" @click="loadDocs">
              <Icon name="refresh" :size="13" /> 刷新
            </button>
            <input
              ref="fileInput"
              type="file"
              style="display: none"
              accept=".pdf,.txt,.md,.markdown,.docx"
              @change="onFileChange"
            />
          </div>

          <div v-if="docsLoading" class="loading-row" style="padding: 40px 0">
            <div class="spinner"></div>
          </div>
          <EmptyState
            v-else-if="docs.records.length === 0"
            title="还没有上传文档"
            description="支持 PDF / DOCX / TXT / MD，最大 20MB"
            icon="file"
          />
          <table v-else class="table">
            <thead>
              <tr>
                <th>文件</th>
                <th>大小</th>
                <th>状态</th>
                <th>切片数</th>
                <th>上传时间</th>
                <th style="text-align: right">动作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in docs.records" :key="d.id">
                <td>
                  <div style="display: flex; align-items: center; gap: 8px">
                    <Icon name="file" :size="16" />
                    <span style="font-weight: 500">{{ d.originalFilename }}</span>
                  </div>
                </td>
                <td>{{ formatFileSize(d.fileSize) }}</td>
                <td>
                  <StatusChip :tone="d.status === 'VECTORIZED' ? 'success' : 'warning'" dot>
                    {{ d.statusName || d.status }}
                  </StatusChip>
                </td>
                <td>{{ d.chunkCount ?? 0 }}</td>
                <td class="text-faint" style="font-size: var(--fs-xs)">{{ fromNow(d.createTime) }}</td>
                <td style="text-align: right; white-space: nowrap">
                  <button class="btn btn--text btn--sm" @click="parse(d)">解析</button>
                  <button class="btn btn--text btn--sm" @click="chunk(d)">切片</button>
                  <button class="btn btn--text btn--sm" @click="vectorize(d)">向量化</button>
                  <button
                    class="btn btn--text btn--sm doc-delete"
                    @click="deleteDoc(d)"
                  >删除</button>
                </td>
              </tr>
            </tbody>
          </table>

          <Pagination
            v-if="docs.total > 0"
            :page-no="docs.pageNo"
            :page-size="docs.pageSize"
            :total="docs.total"
            @change="(p) => (docsPage = p)"
          />
        </template>
      </section>
    </div>

    <!-- 创建/编辑 知识库 Modal -->
    <Modal v-model="baseFormOpen" :title="baseEditing ? '编辑知识库' : '新建知识库'" width="480px">
      <div class="field">
        <label class="field__label">名称 *</label>
        <input v-model.trim="baseForm.name" class="input" placeholder="如：售后政策库" />
      </div>
      <div class="field">
        <label class="field__label">描述</label>
        <textarea v-model="baseForm.description" class="textarea" placeholder="简要说明该知识库覆盖的范围" />
      </div>
      <template #footer>
        <button class="btn btn--ghost" @click="baseFormOpen = false">取消</button>
        <button class="btn btn--primary" @click="submitBase">保存</button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.kb-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
@media (max-width: 900px) {
  .kb-grid { grid-template-columns: 1fr; }
}
.kb-aside { padding: 14px; }
.kb-aside__head {
  display: flex; justify-content: space-between; align-items: center;
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
  font-weight: 600;
  padding: 4px 6px 10px;
  border-bottom: 1px solid var(--border-soft);
  margin-bottom: 8px;
}
.kb-list { display: flex; flex-direction: column; gap: 4px; }
.kb-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.kb-item:hover { background: var(--color-primary-50); }
.kb-item.active {
  background: linear-gradient(135deg, rgba(169,215,242,0.35), rgba(214,204,245,0.35));
}
.kb-item__icon {
  width: 32px; height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #DCEEFB, #C4DEF6);
  color: var(--color-primary-600);
  display: inline-flex; align-items: center; justify-content: center;
}
.kb-item__name {
  font-weight: 600;
  font-size: var(--fs-sm);
  color: var(--color-ink-900);
}
.kb-item__sub {
  margin-top: 4px;
  font-size: var(--fs-xs);
  display: flex; gap: 6px; align-items: center;
}

.kb-main { padding: 18px 22px; }
.kb-main__head {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
  margin-bottom: 14px;
}
.kb-main__title {
  font-size: var(--fs-xl);
  font-weight: 700;
  letter-spacing: -0.01em;
}
.kb-main__sub {
  margin-top: 4px;
  font-size: var(--fs-sm);
  color: var(--color-ink-500);
}
.kb-main__actions { display: flex; gap: 6px; }
.kb-main__toolbar {
  display: flex; gap: 6px; margin: 8px 0 18px;
}
.loading-row { display: flex; align-items: center; justify-content: center; gap: 10px; }
.empty-state { padding: 40px 0; }
.doc-delete { color: var(--color-danger, #c0392b); }
.doc-delete:hover { color: #a93226; background: rgba(192, 57, 43, 0.08); }
</style>
