<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSopStore } from '@/stores/sopStore'
import { getSopVersion } from '@/api/sop'
import type { SopStep } from '@/api/sop'
import { formatDateTime, formatRelative, statusLabel } from './utils'
import type { SopEditorSeed } from './SopEditor.vue'

const props = defineProps<{ sopId: number }>()

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'edit', seed: SopEditorSeed): void
  (e: 'run', id: number, runId: number): void
  (e: 'removed'): void
}>()

const store = useSopStore()

const loading = ref(false)
const starting = ref(false)
const loadFailed = ref(false)
const expandedVersionId = ref<number | null>(null)
const snapshotLoading = ref(false)
const snapshots = ref<Record<number, SopStep[] | null>>({})

const detail = computed(() => store.detail)
const stats = computed(() => detail.value?.stats ?? null)

onMounted(async () => {
  loading.value = true
  try {
    await store.fetchDetail(props.sopId, true)
    await store.fetchRuns(props.sopId)
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
})

/** 编辑种子：把详情数据直接交给编辑器预填 */
function toSeed(): SopEditorSeed | null {
  const d = detail.value
  if (!d) return null
  return {
    id: d.id,
    title: d.title,
    category: d.category,
    triggerScene: d.triggerScene,
    goal: d.goal,
    steps: d.steps,
    useCount: d.useCount
  }
}

function handleEdit(): void {
  const seed = toSeed()
  if (seed) emit('edit', seed)
}

async function handleStartRun(): Promise<void> {
  if (starting.value) return
  starting.value = true
  try {
    const runId = await store.startRun(props.sopId)
    emit('run', props.sopId, runId)
  } catch {
    // 全局拦截器已提示
  } finally {
    starting.value = false
  }
}

async function handleDelete(): Promise<void> {
  const d = detail.value
  if (!d) return
  try {
    await ElMessageBox.confirm(`确定删除《${d.title}》吗？删除后不可恢复。`, '删除 SOP', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await store.removeAndRefresh(props.sopId)
    ElMessage.success('已删除')
    emit('removed')
  } catch {
    // 全局拦截器已提示
  }
}

/** 展开/收起版本快照回看 */
async function toggleVersion(versionId: number): Promise<void> {
  if (expandedVersionId.value === versionId) {
    expandedVersionId.value = null
    return
  }
  expandedVersionId.value = versionId
  if (snapshots.value[versionId] !== undefined) return

  snapshotLoading.value = true
  snapshots.value = { ...snapshots.value, [versionId]: null }
  try {
    // 后端 SopVersionDetailVO 已把 contentJson 解析成结构化 steps，直接消费
    const res = await getSopVersion(props.sopId, versionId)
    snapshots.value = { ...snapshots.value, [versionId]: res.steps ?? [] }
  } catch {
    snapshots.value = { ...snapshots.value, [versionId]: [] }
  } finally {
    snapshotLoading.value = false
  }
}

function snapshotSteps(versionId: number): SopStep[] {
  return snapshots.value[versionId] ?? []
}
</script>

<template>
  <div class="sop-detail">
    <header class="detail-head glass glass--thick">
      <button type="button" class="back-btn glass glass--thin" aria-label="返回列表" @click="emit('back')">
        <svg
          viewBox="0 0 24 24"
          width="18"
          height="18"
          aria-hidden="true"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M14.5 6 8.5 12l6 6" />
        </svg>
      </button>
      <div class="detail-head__text">
        <div class="detail-head__row">
          <h1 class="detail-head__title">{{ detail?.title || '加载中…' }}</h1>
          <span v-if="detail?.pinned" class="pin glass glass--thin" title="已置顶">置顶</span>
        </div>
        <p class="detail-head__sub">
          <span v-if="detail" class="head-chip">{{ detail.category || '未分类' }}</span>
          <span v-if="detail" class="head-chip">{{ detail.version }}</span>
          <span v-if="detail" class="head-chip">{{ statusLabel(detail.status) }}</span>
          <span v-if="detail" class="head-chip">{{ detail.steps.length }} 步</span>
        </p>
      </div>
    </header>

    <!-- 加载态 -->
    <section v-if="loading" class="panel glass glass--thick" aria-busy="true">
      <p class="muted">正在加载 SOP…</p>
    </section>

    <!-- 加载失败 / 不存在 -->
    <section v-else-if="loadFailed || !detail" class="panel glass glass--thick empty-inline">
      <p class="empty-inline__title">没找到这个 SOP</p>
      <p class="muted">它可能已被删除，或后端服务暂时不可用。</p>
      <button type="button" class="btn-ghost glass glass--thin" @click="emit('back')">返回列表</button>
    </section>

    <template v-else>
      <!-- 统计：跑过几次 / 平均多久 / 常卡哪步 -->
      <section class="stats" aria-label="执行统计">
        <div class="stat-card glass">
          <span class="stat-card__label">跑过几次</span>
          <span class="stat-card__num">{{ detail.useCount ?? 0 }}</span>
        </div>
        <div class="stat-card glass">
          <span class="stat-card__label">平均多久</span>
          <span class="stat-card__num">{{ detail.avgMinutes || 0 }}<i class="stat-card__unit">分</i></span>
        </div>
        <div class="stat-card glass">
          <span class="stat-card__label">常卡哪步</span>
          <span class="stat-card__num stat-card__num--sm">
            {{ stats?.topStuckStep ? `第 ${stats.topStuckStep} 步` : '无' }}
          </span>
        </div>
        <div class="stat-card glass">
          <span class="stat-card__label">最近使用</span>
          <span class="stat-card__num stat-card__num--sm">{{ formatRelative(detail.lastUsedAt) }}</span>
        </div>
      </section>

      <!-- 场景 / 目标 -->
      <section
        v-if="detail.triggerScene || detail.goal"
        class="panel glass glass--thick"
        aria-label="使用说明"
      >
        <div v-if="detail.triggerScene" class="info-row">
          <span class="info-row__label">什么场景下用</span>
          <p class="info-row__value">{{ detail.triggerScene }}</p>
        </div>
        <div v-if="detail.goal" class="info-row">
          <span class="info-row__label">产出什么结果</span>
          <p class="info-row__value">{{ detail.goal }}</p>
        </div>
      </section>

      <!-- 步骤清单 -->
      <section class="panel glass glass--thick" aria-label="步骤清单">
        <h2 class="panel__title">步骤清单</h2>
        <ol class="steps" role="list">
          <li v-for="step in detail.steps" :key="step.stepNo" class="step-item glass">
            <div class="step-item__head">
              <span class="step-item__no" aria-hidden="true">{{ step.stepNo }}</span>
              <h3 class="step-item__title">{{ step.title }}</h3>
              <span v-if="step.estimateMin" class="step-item__est">约 {{ step.estimateMin }} 分</span>
            </div>
            <p v-if="step.detail" class="step-item__detail">{{ step.detail }}</p>
            <div v-if="step.tip" class="tip">
              <svg
                class="tip__icon"
                viewBox="0 0 24 24"
                width="16"
                height="16"
                aria-hidden="true"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M12 4.6 20 18.4H4z" />
                <path d="M12 9.6v3.6" />
                <path d="M12 15.8h.01" />
              </svg>
              <span class="tip__text">{{ step.tip }}</span>
            </div>
          </li>
        </ol>
      </section>

      <!-- 版本时间线 -->
      <section class="panel glass glass--thick" aria-label="版本时间线">
        <h2 class="panel__title">版本时间线</h2>
        <p v-if="!detail.versions.length" class="muted">
          还没有历史版本 —— 执行过之后再编辑，就会自动留档。
        </p>
        <ul v-else class="timeline" role="list">
          <li v-for="version in detail.versions" :key="version.id" class="timeline__item">
            <span class="timeline__dot" aria-hidden="true" />
            <div class="timeline__body">
              <div class="timeline__head">
                <span class="timeline__ver">{{ version.version }}</span>
                <span class="timeline__time">{{ formatDateTime(version.createTime) }}</span>
                <button
                  type="button"
                  class="timeline__toggle"
                  :aria-expanded="expandedVersionId === version.id"
                  @click="toggleVersion(version.id)"
                >
                  {{ expandedVersionId === version.id ? '收起' : '回看' }}
                </button>
              </div>
              <p v-if="version.changeNote" class="timeline__note">{{ version.changeNote }}</p>

              <div v-if="expandedVersionId === version.id" class="snapshot glass">
                <p v-if="snapshotLoading" class="muted">快照加载中…</p>
                <template v-else-if="snapshotSteps(version.id).length">
                  <p class="snapshot__title">该版本的步骤（{{ snapshotSteps(version.id).length }}）</p>
                  <ol class="snapshot__list" role="list">
                    <li v-for="s in snapshotSteps(version.id)" :key="s.stepNo" class="snapshot__step">
                      <span class="snapshot__no">{{ s.stepNo }}</span>
                      <div>
                        <span class="snapshot__step-title">{{ s.title }}</span>
                        <p v-if="s.detail" class="snapshot__step-detail">{{ s.detail }}</p>
                        <p v-if="s.tip" class="snapshot__step-tip">避坑：{{ s.tip }}</p>
                      </div>
                    </li>
                  </ol>
                </template>
                <p v-else class="muted">该版本没有可回看的步骤快照。</p>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <!-- 执行历史 -->
      <section class="panel glass glass--thick" aria-label="执行历史">
        <div class="panel__head">
          <h2 class="panel__title">执行历史</h2>
          <span class="panel__hint">共 {{ store.runsTotal }} 次</span>
        </div>
        <p v-if="!store.runs.length" class="muted">还没有执行记录，点下面的「开始执行」跑一次吧。</p>
        <ul v-else class="runs" role="list">
          <li v-for="run in store.runs" :key="run.id" class="run-row glass">
            <span class="run-row__time">{{ formatDateTime(run.startedAt) }}</span>
            <span class="run-row__cost">
              {{ run.costMin != null ? `${run.costMin} 分` : '进行中' }}
            </span>
            <span class="run-row__stuck">
              {{ run.stuckStep ? `卡在第 ${run.stuckStep} 步` : '无卡点' }}
            </span>
            <span class="run-row__dev" :title="run.deviation || ''">
              {{ run.deviation || '—' }}
            </span>
          </li>
        </ul>
      </section>

      <!-- 底部操作 -->
      <footer class="detail-foot">
        <button type="button" class="btn-ghost glass glass--thin" @click="handleDelete">删除</button>
        <button type="button" class="btn-ghost glass glass--thin" @click="handleEdit">编辑</button>
        <button
          type="button"
          class="btn-primary"
          :disabled="starting || !detail.steps.length"
          :aria-busy="starting"
          @click="handleStartRun"
        >
          <span v-if="starting" class="spinner" aria-hidden="true" />
          <svg
            v-else
            viewBox="0 0 24 24"
            width="18"
            height="18"
            aria-hidden="true"
            fill="currentColor"
          >
            <path d="M8 5.6v12.8l10.4-6.4z" />
          </svg>
          <span>{{ starting ? '准备中…' : '开始执行' }}</span>
        </button>
      </footer>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.sop-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---- 头部 ---- */
.detail-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px 22px;
  border-radius: var(--r-xl);
  @include enter-rise;
}

.back-btn {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  color: var(--text-primary);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.detail-head__text {
  min-width: 0;
  flex: 1 1 auto;
}

.detail-head__row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-head__title {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--text-primary);
}

.pin {
  flex: 0 0 auto;
  padding: 3px 10px;
  font-size: 11px;
  color: var(--accent);
  border-radius: var(--r-pill);
}

.detail-head__sub {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.head-chip {
  padding: 3px 10px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
}

/* ---- 面板 ---- */
.panel {
  padding: 20px 22px;
  border-radius: var(--r-lg);
  @include enter-rise;
}

.panel__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.panel__title {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.panel__head .panel__title {
  margin-bottom: 14px;
}

.panel__hint {
  font-size: 12px;
  color: var(--text-tertiary);
}

.muted {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---- 统计 ---- */
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 18px;
  border-radius: var(--r-md);
}

.stat-card__label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.stat-card__num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.4px;
  color: var(--accent);
}

.stat-card__num--sm {
  font-size: 18px;
  color: var(--text-primary);
}

.stat-card__unit {
  margin-left: 3px;
  font-size: 12px;
  font-style: normal;
  font-weight: 500;
  color: var(--text-tertiary);
}

/* ---- 说明行 ---- */
.info-row + .info-row {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--separator);
}

.info-row__label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-tertiary);
}

.info-row__value {
  margin-top: 6px;
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-primary);
}

/* ---- 步骤清单 ---- */
.steps {
  display: flex;
  flex-direction: column;
  gap: 10px;
  list-style: none;
}

.step-item {
  padding: 14px 16px;
  border-radius: var(--r-md);
}

.step-item__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.step-item__no {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 60%, var(--accent-press) 100%);
  border-radius: var(--r-pill);
}

.step-item__title {
  flex: 1 1 auto;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.step-item__est {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--text-tertiary);
}

.step-item__detail {
  margin-top: 8px;
  margin-left: 38px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  color: var(--text-secondary);
}

/* ---- 避坑黄条（明暗两套） ---- */
.tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
  margin-left: 38px;
  padding: 9px 12px;
  background: var(--warn-bg);
  border: 1px solid var(--warn-border);
  border-left: 3px solid var(--warn-border-strong);
  border-radius: var(--r-sm);
}

.tip__icon {
  flex: 0 0 auto;
  margin-top: 1px;
  color: var(--warn-icon);
}

.tip__text {
  font-size: 13px;
  line-height: 1.55;
  color: var(--warn-text);
}

/* ---- 版本时间线 ---- */
.timeline {
  list-style: none;
}

.timeline__item {
  position: relative;
  padding-left: 22px;
  padding-bottom: 16px;
}

.timeline__item::before {
  content: '';
  position: absolute;
  top: 14px;
  bottom: -2px;
  left: 5px;
  width: 1px;
  background: var(--separator);
}

.timeline__item:last-child::before {
  display: none;
}

.timeline__dot {
  position: absolute;
  top: 5px;
  left: 0;
  width: 11px;
  height: 11px;
  background: var(--accent);
  border: 2px solid var(--glass-border);
  border-radius: 50%;
}

.timeline__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.timeline__ver {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.timeline__time {
  flex: 1 1 auto;
  font-size: 12px;
  color: var(--text-tertiary);
}

.timeline__toggle {
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  color: var(--accent);
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.timeline__note {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.snapshot {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: var(--r-sm);
}

.snapshot__title {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-tertiary);
}

.snapshot__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  list-style: none;
}

.snapshot__step {
  display: flex;
  gap: 9px;
}

.snapshot__no {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  color: var(--text-secondary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
}

.snapshot__step-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.snapshot__step-detail {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-secondary);
}

.snapshot__step-tip {
  margin-top: 2px;
  font-size: 12px;
  color: var(--warn-text);
}

/* ---- 执行历史 ---- */
.runs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  list-style: none;
}

.run-row {
  display: grid;
  grid-template-columns: 1.4fr 0.7fr 0.9fr 1.6fr;
  gap: 10px;
  align-items: center;
  padding: 10px 14px;
  font-size: 13px;
  border-radius: var(--r-sm);
}

.run-row__time {
  color: var(--text-primary);
}

.run-row__cost {
  font-weight: 600;
  color: var(--accent);
}

.run-row__stuck {
  color: var(--text-secondary);
}

.run-row__dev {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-tertiary);
}

/* ---- 空内联 ---- */
.empty-inline {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 24px;
  text-align: center;
}

.empty-inline__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

/* ---- 底部操作 ---- */
.detail-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-ghost {
  height: 48px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-width: 140px;
  height: 48px;
  padding: 0 26px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 55%, var(--accent-press) 100%);
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.42);
  @include pressable;
}

.btn-primary:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.spinner {
  width: 17px;
  height: 17px;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: #fff;
  border-radius: 50%;
}

@media (prefers-reduced-motion: no-preference) {
  .spinner {
    animation: po-spin 0.7s linear infinite;
  }
}

@keyframes po-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .run-row {
    grid-template-columns: 1fr 1fr;
    row-gap: 4px;
  }

  .run-row__dev {
    grid-column: 1 / -1;
  }
}

@media (max-width: 480px) {
  .detail-head {
    padding: 16px 18px;
    border-radius: var(--r-lg);
  }

  .detail-head__title {
    font-size: 20px;
  }

  .panel {
    padding: 16px 18px;
  }

  .step-item__detail,
  .tip {
    margin-left: 0;
  }

  .detail-foot {
    flex-direction: column-reverse;
  }

  .btn-ghost,
  .btn-primary {
    width: 100%;
  }
}
</style>
