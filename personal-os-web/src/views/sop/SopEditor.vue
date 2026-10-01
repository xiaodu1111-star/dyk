<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSopStore } from '@/stores/sopStore'
import { useUserStore } from '@/stores/user'
import type { SopSavePayload, SopStep } from '@/api/sop'
import { CATEGORY_SUGGESTIONS } from './utils'

/** 从详情进入编辑时的预填种子（避免后端未就绪时再发一次请求） */
export interface SopEditorSeed {
  id: number
  title: string
  category: string | null
  triggerScene: string | null
  goal: string | null
  steps: SopStep[]
  useCount: number
}

const props = defineProps<{
  sopId: number | null
  seed?: SopEditorSeed | null
}>()

const emit = defineEmits<{
  (e: 'saved', id: number | null): void
  (e: 'cancel'): void
}>()

const store = useSopStore()
const userStore = useUserStore()

/** 行内步骤草稿 */
interface StepDraft {
  key: number
  title: string
  detail: string
  tip: string
  estimateMin: string
}

let keySeq = 0
function nextKey(): number {
  keySeq += 1
  return keySeq
}

function blankStep(): StepDraft {
  return { key: nextKey(), title: '', detail: '', tip: '', estimateMin: '' }
}

const form = reactive({
  title: '',
  category: '',
  triggerScene: '',
  goal: '',
  changeNote: ''
})

const steps = ref<StepDraft[]>([blankStep()])
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const titleInvalid = ref(false)
const stepsInvalid = ref(false)

/** 编辑态（有 id） */
const isEdit = computed(() => props.sopId != null)
/** 该 SOP 是否已被执行过 —— 决定是否提示"将留版本快照" */
const willVersion = ref(false)
/** 计划总耗时预览 */
const plannedTotal = computed(() =>
  steps.value.reduce((sum, s) => sum + (Number(s.estimateMin) || 0), 0)
)

onMounted(async () => {
  if (props.seed) {
    applySeed(props.seed)
    return
  }
  if (props.sopId == null) {
    return
  }
  loading.value = true
  try {
    const detail = await store.fetchDetail(props.sopId, true)
    if (detail) {
      applySeed({
        id: detail.id,
        title: detail.title,
        category: detail.category,
        triggerScene: detail.triggerScene,
        goal: detail.goal,
        steps: detail.steps,
        useCount: detail.useCount
      })
    }
  } catch {
    // 全局拦截器已提示；保留空表单
  } finally {
    loading.value = false
  }
})

function applySeed(seed: SopEditorSeed): void {
  form.title = seed.title ?? ''
  form.category = seed.category ?? ''
  form.triggerScene = seed.triggerScene ?? ''
  form.goal = seed.goal ?? ''
  willVersion.value = (seed.useCount ?? 0) > 0
  steps.value = seed.steps?.length
    ? seed.steps.map((s) => ({
        key: nextKey(),
        title: s.title ?? '',
        detail: s.detail ?? '',
        tip: s.tip ?? '',
        estimateMin: s.estimateMin == null ? '' : String(s.estimateMin)
      }))
    : [blankStep()]
}

function clearError(): void {
  errorMessage.value = ''
  titleInvalid.value = false
  stepsInvalid.value = false
}

// ---- 步骤编辑 ----
function addStep(): void {
  steps.value.push(blankStep())
}

function removeStep(index: number): void {
  if (steps.value.length <= 1) {
    steps.value = [blankStep()]
    return
  }
  steps.value.splice(index, 1)
}

function moveStep(index: number, delta: number): void {
  const target = index + delta
  if (target < 0 || target >= steps.value.length) return
  const [item] = steps.value.splice(index, 1)
  steps.value.splice(target, 0, item)
}

function validate(): boolean {
  clearError()
  if (!form.title.trim()) {
    errorMessage.value = '请填写 SOP 标题'
    titleInvalid.value = true
    return false
  }
  const filled = steps.value.filter((s) => s.title.trim())
  if (filled.length === 0) {
    errorMessage.value = '至少填写 1 个步骤标题'
    stepsInvalid.value = true
    return false
  }
  if (steps.value.some((s) => !s.title.trim())) {
    errorMessage.value = '有步骤还没填标题，请补全或删除该步骤'
    stepsInvalid.value = true
    return false
  }
  return true
}

function buildPayload(): SopSavePayload {
  return {
    title: form.title.trim(),
    category: form.category.trim() || null,
    triggerScene: form.triggerScene.trim() || null,
    goal: form.goal.trim() || null,
    sourceTaskId: null,
    changeNote: form.changeNote.trim() || null,
    steps: steps.value.map((s) => ({
      title: s.title.trim(),
      detail: s.detail.trim() || null,
      tip: s.tip.trim() || null,
      estimateMin: s.estimateMin === '' ? null : Number(s.estimateMin)
    }))
  }
}

async function handleSave(): Promise<void> {
  if (saving.value || !validate()) return

  // 编辑已执行过的 SOP：显式确认"将自动留版本快照"
  if (isEdit.value && willVersion.value) {
    try {
      await ElMessageBox.confirm(
        '这个 SOP 已经被执行过，保存时会自动留一份版本快照（版本号 +0.1），之后可以在详情页回看旧版本。',
        '将自动留版本快照',
        {
          confirmButtonText: '保存并留档',
          cancelButtonText: '再想想',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }

  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value && props.sopId != null) {
      await store.updateAndRefresh(props.sopId, payload)
      ElMessage.success(willVersion.value ? '已保存，旧版本已留档' : '已保存')
      emit('saved', props.sopId)
    } else {
      const id = await store.createAndRefresh(payload)
      ElMessage.success('SOP 已创建')
      emit('saved', id)
    }
  } catch {
    // 全局拦截器已提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="sop-editor">
    <header class="editor-head glass glass--thick">
      <button type="button" class="back-btn glass glass--thin" aria-label="返回" @click="emit('cancel')">
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
      <div class="editor-head__text">
        <h1 class="editor-head__title">{{ isEdit ? '编辑 SOP' : '新建 SOP' }}</h1>
        <p class="editor-head__subtitle">把流程拆成可执行的小步，写下避坑提醒</p>
      </div>
    </header>

    <Transition name="po-alert">
      <div v-if="errorMessage" class="alert alert--danger" role="alert" aria-live="assertive">
        <svg
          class="alert__icon"
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7.6v5.2" />
          <path d="M12 16.4h.01" />
        </svg>
        <span>{{ errorMessage }}</span>
      </div>
    </Transition>

    <div v-if="isEdit && willVersion" class="alert alert--warn" role="status">
      <svg
        class="alert__icon"
        viewBox="0 0 24 24"
        width="18"
        height="18"
        fill="none"
        stroke="currentColor"
        stroke-width="1.8"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M12 4.6 20 18.4H4z" />
        <path d="M12 9.6v3.6" />
        <path d="M12 15.8h.01" />
      </svg>
      <span>这个 SOP 已被执行过，保存将自动留一份版本快照（版本号 +0.1）。</span>
    </div>

    <section class="panel glass glass--thick" aria-label="基本信息">
      <h2 class="panel__title">基本信息</h2>

      <div class="field">
        <label class="field__label" for="sop-title">
          标题 <span class="field__req" aria-hidden="true">*</span>
        </label>
        <input
          id="sop-title"
          v-model="form.title"
          class="input"
          type="text"
          maxlength="200"
          placeholder="如：需求评审会前的准备"
          :aria-invalid="titleInvalid"
          @input="clearError"
        />
      </div>

      <div class="grid2">
        <div class="field">
          <label class="field__label" for="sop-category">分类</label>
          <input
            id="sop-category"
            v-model="form.category"
            class="input"
            type="text"
            maxlength="50"
            placeholder="如：需求评审"
          />
          <div class="suggest">
            <button
              v-for="cat in CATEGORY_SUGGESTIONS"
              :key="cat"
              type="button"
              class="suggest__item"
              @click="form.category = cat"
            >
              {{ cat }}
            </button>
          </div>
        </div>
      </div>

      <div class="field">
        <label class="field__label" for="sop-scene">什么场景下用</label>
        <input
          id="sop-scene"
          v-model="form.triggerScene"
          class="input"
          type="text"
          maxlength="500"
          placeholder="如：每次拿到新需求、准备排期之前"
        />
      </div>

      <div class="field">
        <label class="field__label" for="sop-goal">产出什么结果</label>
        <input
          id="sop-goal"
          v-model="form.goal"
          class="input"
          type="text"
          maxlength="500"
          placeholder="如：一份确认过的需求清单 + 排期"
        />
      </div>

      <div v-if="isEdit" class="field">
        <label class="field__label" for="sop-note">本次优化了什么（可选）</label>
        <input
          id="sop-note"
          v-model="form.changeNote"
          class="input"
          type="text"
          maxlength="500"
          placeholder="如：把第 2 步的确认清单改细了"
        />
      </div>
    </section>

    <section class="panel glass glass--thick" aria-label="步骤编辑器">
      <div class="panel__head">
        <h2 class="panel__title">步骤</h2>
        <span class="panel__hint">
          {{ steps.length }} 步 · 共 {{ plannedTotal }} 分钟
        </span>
      </div>

      <TransitionGroup name="po-step" tag="div" class="step-list">
        <article
          v-for="(step, index) in steps"
          :key="step.key"
          class="step-card glass"
          :class="{ 'step-card--invalid': stepsInvalid && !step.title.trim() }"
        >
          <header class="step-card__head">
            <span class="step-no" aria-hidden="true">{{ index + 1 }}</span>
            <div class="step-card__tools">
              <button
                type="button"
                class="mini-btn"
                :disabled="index === 0"
                aria-label="上移"
                title="上移"
                @click="moveStep(index, -1)"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <path d="m7 13.5 5-5 5 5" />
                </svg>
              </button>
              <button
                type="button"
                class="mini-btn"
                :disabled="index === steps.length - 1"
                aria-label="下移"
                title="下移"
                @click="moveStep(index, 1)"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <path d="m7 10.5 5 5 5-5" />
                </svg>
              </button>
              <button
                type="button"
                class="mini-btn mini-btn--danger"
                aria-label="删除该步骤"
                title="删除该步骤"
                @click="removeStep(index)"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M5.4 7.4h13.2" />
                  <path d="M7 7.4l.9 11.2a1.6 1.6 0 0 0 1.6 1.5h5a1.6 1.6 0 0 0 1.6-1.5L17 7.4" />
                  <path d="M9.4 7.4V5.6a1.4 1.4 0 0 1 1.4-1.4h2.4a1.4 1.4 0 0 1 1.4 1.4v1.8" />
                </svg>
              </button>
            </div>
          </header>

          <div class="field">
            <label class="sr-only" :for="`step-title-${step.key}`">步骤标题</label>
            <input
              :id="`step-title-${step.key}`"
              v-model="step.title"
              class="input"
              type="text"
              maxlength="200"
              placeholder="这一步做什么"
              @input="clearError"
            />
          </div>

          <div class="field">
            <label class="sr-only" :for="`step-detail-${step.key}`">操作说明</label>
            <textarea
              :id="`step-detail-${step.key}`"
              v-model="step.detail"
              class="input input--area"
              rows="2"
              maxlength="2000"
              placeholder="具体怎么做（可选）"
            />
          </div>

          <div class="grid2">
            <div class="field">
              <label class="sr-only" :for="`step-tip-${step.key}`">避坑提示</label>
              <textarea
                :id="`step-tip-${step.key}`"
                v-model="step.tip"
                class="input input--area input--tip"
                rows="2"
                maxlength="500"
                placeholder="避坑提示（可选）"
              />
            </div>
            <div class="field">
              <label class="field__label" :for="`step-est-${step.key}`">预估（分钟）</label>
              <input
                :id="`step-est-${step.key}`"
                v-model="step.estimateMin"
                class="input"
                type="number"
                min="0"
                step="1"
                placeholder="如 10"
              />
            </div>
          </div>
        </article>
      </TransitionGroup>

      <button type="button" class="add-step" @click="addStep">
        <svg
          viewBox="0 0 24 24"
          width="17"
          height="17"
          aria-hidden="true"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
        >
          <path d="M12 5.4v13.2M5.4 12h13.2" />
        </svg>
        添加步骤
      </button>
    </section>

    <footer class="editor-foot">
      <button type="button" class="btn-ghost glass glass--thin" :disabled="saving" @click="emit('cancel')">
        取消
      </button>
      <button type="button" class="btn-primary" :disabled="saving" :aria-busy="saving" @click="handleSave">
        <span v-if="saving" class="spinner" aria-hidden="true" />
        <span>{{ saving ? '保存中…' : isEdit ? '保存' : '创建 SOP' }}</span>
      </button>
    </footer>

    <p class="editor-foot__who">创建人：{{ userStore.displayName || '当前用户' }}</p>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.sop-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---- 头部 ---- */
.editor-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 22px;
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

.editor-head__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}

.editor-head__subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---- 提示条 ---- */
.alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 11px 15px;
  font-size: 13px;
  border-radius: var(--r-md);
}

.alert__icon {
  flex: 0 0 auto;
  margin-top: 1px;
}

.alert--danger {
  color: var(--danger);
  background: rgba(255, 59, 48, 0.12);
  border: 1px solid rgba(255, 59, 48, 0.28);
}

.alert--warn {
  color: var(--warn-text);
  background: var(--warn-bg);
  border: 1px solid var(--warn-border);
  border-left: 3px solid var(--warn-border-strong);
}

@media (prefers-reduced-motion: no-preference) {
  .po-alert-enter-active,
  .po-alert-leave-active {
    transition: opacity var(--dur) var(--spring), transform var(--dur) var(--spring);
  }

  .po-alert-enter-from,
  .po-alert-leave-to {
    opacity: 0;
    transform: translateY(-8px);
  }
}

/* ---- 面板 ---- */
.panel {
  padding: 22px;
  border-radius: var(--r-lg);
  @include enter-rise;
}

.panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel__title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.panel__head .panel__title {
  margin-bottom: 0;
}

.panel__hint {
  margin-bottom: 16px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.panel__head + .step-list {
  margin-top: 14px;
}

/* ---- 表单字段 ---- */
.field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.field + .field,
.grid2 + .field,
.field + .grid2 {
  margin-top: 14px;
}

.field__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.field__req {
  color: var(--danger);
}

.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 14px;
}

.grid2 .field {
  margin-top: 0;
}

.input {
  width: 100%;
  height: 46px;
  padding: 0 15px;
  font-size: 15px;
  font-family: var(--font-sans);
  color: var(--text-primary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-md);
  outline: none;
  transition: border-color var(--dur) var(--spring), box-shadow var(--dur) var(--spring);
}

.input::placeholder {
  color: var(--text-tertiary);
}

.input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.18);
}

.input[aria-invalid='true'] {
  border-color: var(--danger);
}

.input--area {
  height: auto;
  padding: 11px 15px;
  line-height: 1.5;
  resize: vertical;
}

.input--tip {
  border-left: 3px solid var(--warn-border-strong);
}

.suggest {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 2px;
}

.suggest__item {
  height: 28px;
  padding: 0 11px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring), border-color var(--dur) var(--spring);
}

.suggest__item:hover {
  color: var(--accent);
  border-color: var(--accent);
}

/* ---- 步骤卡 ---- */
.step-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-card {
  padding: 16px 18px;
  border-radius: var(--r-md);
}

.step-card--invalid {
  border-color: var(--danger);
}

.step-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.step-no {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  padding: 0 9px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 60%, var(--accent-press) 100%);
  border-radius: var(--r-pill);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.42);
}

.step-card__tools {
  display: inline-flex;
  gap: 4px;
}

.mini-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring), background-color var(--dur) var(--spring),
    transform var(--dur-fast) var(--spring);
}

@media (prefers-reduced-motion: no-preference) {
  .mini-btn:active:not(:disabled) {
    transform: scale(0.97);
  }
}

.mini-btn:hover:not(:disabled) {
  color: var(--text-primary);
  background: var(--glass-bg-thin);
}

.mini-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.mini-btn--danger:hover:not(:disabled) {
  color: var(--danger);
}

.step-card .field + .field,
.step-card .field + .grid2 {
  margin-top: 10px;
}

.step-card .grid2 {
  margin-top: 10px;
}

@media (prefers-reduced-motion: no-preference) {
  .po-step-enter-active,
  .po-step-leave-active {
    transition: opacity var(--dur) var(--spring), transform var(--dur) var(--spring);
  }

  .po-step-enter-from,
  .po-step-leave-to {
    opacity: 0;
    transform: translateY(-8px);
  }

  .po-step-move {
    transition: transform var(--dur-slow) var(--spring);
  }
}

/* ---- 加步骤 ---- */
.add-step {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  margin-top: 14px;
  font-size: 15px;
  font-weight: 500;
  color: var(--accent);
  background: var(--glass-bg-thin);
  border: 1px dashed var(--glass-border);
  border-radius: var(--r-md);
  cursor: pointer;
  @include pressable;
}

/* ---- 底部操作 ---- */
.editor-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-ghost {
  height: 48px;
  padding: 0 24px;
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
  gap: 10px;
  min-width: 132px;
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

.btn-primary:disabled,
.btn-ghost:disabled {
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

.editor-foot__who {
  font-size: 12px;
  text-align: right;
  color: var(--text-tertiary);
}

@media (max-width: 480px) {
  .editor-head {
    padding: 16px 18px;
    border-radius: var(--r-lg);
  }

  .editor-head__title {
    font-size: 20px;
  }

  .panel {
    padding: 18px;
  }

  .grid2 {
    grid-template-columns: 1fr;
  }

  .editor-foot {
    flex-direction: column-reverse;
  }

  .btn-ghost,
  .btn-primary {
    width: 100%;
  }
}
</style>
