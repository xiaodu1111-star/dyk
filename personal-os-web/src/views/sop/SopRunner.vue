<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useSopStore } from '@/stores/sopStore'
import { formatDuration } from './utils'

const props = defineProps<{
  sopId: number
  runId: number | null
}>()

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'finished', id: number): void
}>()

const store = useSopStore()

// ---- 执行态 ----
const currentIndex = ref(0)
const elapsed = ref(0)
let timer: number | null = null
const startedAt = Date.now()

// ---- 结束弹窗 ----
const finishVisible = ref(false)
const submitting = ref(false)
const stuckStep = ref<number | null>(null)
const deviation = ref('')

const detail = computed(() => store.detail)
const steps = computed(() => detail.value?.steps ?? [])
const total = computed(() => steps.value.length)
const isFirst = computed(() => currentIndex.value <= 0)
const isLast = computed(() => currentIndex.value >= total.value - 1)
const progress = computed(() =>
  total.value === 0 ? 0 : Math.round((currentIndex.value / total.value) * 100)
)

/** 结束弹窗展示的耗时（秒 → 分钟，向上取整、最小 1） */
const costMinutes = computed(() => Math.max(1, Math.ceil(elapsed.value / 60)))

onMounted(async () => {
  startTimer()
  try {
    await store.fetchDetail(props.sopId, true)
  } catch {
    // 全局拦截器已提示；下方会渲染"没有步骤"态
  }
})

onBeforeUnmount(stopTimer)

function startTimer(): void {
  stopTimer()
  timer = window.setInterval(() => {
    elapsed.value = Math.floor((Date.now() - startedAt) / 1000)
  }, 1000)
}

function stopTimer(): void {
  if (timer != null) {
    window.clearInterval(timer)
    timer = null
  }
}

function goPrev(): void {
  if (!isFirst.value) currentIndex.value -= 1
}

/** 标记当前步完成并前进；最后一步直接打开结束弹窗 */
function goNext(): void {
  if (isLast.value) {
    openFinish()
    return
  }
  currentIndex.value += 1
}

/** 点击已完成步骤：回看/回退到该步 */
function jumpTo(index: number): void {
  if (index <= currentIndex.value) currentIndex.value = index
}

function stepState(index: number): 'done' | 'current' | 'todo' {
  if (index < currentIndex.value) return 'done'
  if (index === currentIndex.value) return 'current'
  return 'todo'
}

function openFinish(): void {
  stopTimer()
  finishVisible.value = true
}

function cancelFinish(): void {
  finishVisible.value = false
  startTimer()
}

async function confirmFinish(): Promise<void> {
  if (submitting.value) return
  submitting.value = true
  const payload = {
    finished: true,
    costMin: costMinutes.value,
    stuckStep: stuckStep.value,
    deviation: deviation.value.trim() || null
  }
  if (props.runId != null) {
    try {
      await store.finishRun(props.runId, payload)
      ElMessage.success('已记录本次执行')
    } catch {
      // 全局拦截器已提示
    }
  } else {
    ElMessage.warning('未拿到执行记录，本次仅本地完成')
  }
  submitting.value = false
  finishVisible.value = false
  emit('finished', props.sopId)
}

/** 放弃本次执行（不记录） */
function handleAbandon(): void {
  emit('back')
}
</script>

<template>
  <div class="sop-runner">
    <!-- 顶栏：返回 + 计时 + 进度 -->
    <header class="runner-bar glass glass--thick">
      <button type="button" class="back-btn glass glass--thin" aria-label="退出执行" @click="handleAbandon">
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
          <path d="M6 6l12 12M18 6 6 18" />
        </svg>
      </button>
      <div class="runner-bar__center">
        <p class="runner-bar__title">{{ detail?.title || '执行中' }}</p>
        <div class="progress" aria-hidden="true">
          <span class="progress__fill" :style="{ width: `${progress}%` }" />
        </div>
      </div>
      <div class="timer glass glass--thin" role="timer" :aria-label="`已用时 ${formatDuration(elapsed)}`">
        <svg
          viewBox="0 0 24 24"
          width="15"
          height="15"
          aria-hidden="true"
          fill="none"
          stroke="currentColor"
          stroke-width="1.7"
          stroke-linecap="round"
        >
          <circle cx="12" cy="13" r="7.4" />
          <path d="M12 9.6V13l2.4 1.6M9.6 3.4h4.8" />
        </svg>
        <span class="timer__text">{{ formatDuration(elapsed) }}</span>
      </div>
    </header>

    <!-- 无步骤兜底 -->
    <section v-if="total === 0" class="panel glass glass--thick empty-inline">
      <p class="empty-inline__title">这个 SOP 还没有步骤</p>
      <p class="muted">先回去补充步骤，再开始执行。</p>
      <button type="button" class="btn-ghost glass glass--thin" @click="emit('back')">返回详情</button>
    </section>

    <!-- 步骤纵向清单 -->
    <ol v-else class="runner-list" role="list">
      <li
        v-for="(step, index) in steps"
        :key="step.stepNo"
        class="run-step glass"
        :class="[
          `run-step--${stepState(index)}`,
          { 'run-step--clickable': stepState(index) === 'done' }
        ]"
        @click="jumpTo(index)"
      >
        <!-- 左侧状态列：对勾 / 序号 -->
        <span class="run-step__status" aria-hidden="true">
          <svg
            v-if="stepState(index) === 'done'"
            viewBox="0 0 24 24"
            width="16"
            height="16"
            fill="none"
            stroke="currentColor"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="m5.4 12.6 4.2 4.2 9-9.4" />
          </svg>
          <span v-else>{{ step.stepNo ?? index + 1 }}</span>
        </span>

        <div class="run-step__body">
          <div class="run-step__head">
            <h2 class="run-step__title">{{ step.title }}</h2>
            <span v-if="step.estimateMin" class="run-step__est">约 {{ step.estimateMin }} 分</span>
          </div>

          <p v-if="step.detail" class="run-step__detail">{{ step.detail }}</p>

          <!-- 避坑黄条（明暗两套） -->
          <div v-if="step.tip" class="run-tip">
            <svg
              class="run-tip__icon"
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
            <div>
              <p class="run-tip__label">避坑</p>
              <p class="run-tip__text">{{ step.tip }}</p>
            </div>
          </div>

          <p v-if="stepState(index) === 'done'" class="run-step__done-hint">已完成</p>
        </div>
      </li>
    </ol>

    <!-- 底部：上一步 / 下一步 玻璃按钮 -->
    <footer v-if="total > 0" class="runner-foot">
      <button
        type="button"
        class="nav-btn glass glass--thin"
        :disabled="isFirst"
        @click="goPrev"
      >
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
        上一步
      </button>
      <button type="button" class="nav-btn nav-btn--primary" @click="goNext">
        <span>{{ isLast ? '完成本次' : '下一步' }}</span>
        <svg
          v-if="!isLast"
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
          <path d="m9.5 6 6 6-6 6" />
        </svg>
        <svg
          v-else
          viewBox="0 0 24 24"
          width="18"
          height="18"
          aria-hidden="true"
          fill="none"
          stroke="currentColor"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="m5.4 12.6 4.2 4.2 9-9.4" />
        </svg>
      </button>
    </footer>

    <!-- 结束弹窗：全屏完成态 -->
    <Transition name="po-finish">
      <div
        v-if="finishVisible"
        class="finish"
        role="dialog"
        aria-modal="true"
        aria-label="本次执行完成"
      >
        <div class="finish__card glass glass--thick">
          <div class="finish__badge glass glass--thin" aria-hidden="true">
            <svg
              viewBox="0 0 24 24"
              width="30"
              height="30"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="m5.4 12.6 4.2 4.2 9-9.4" />
            </svg>
          </div>
          <h2 class="finish__title">本次执行完成</h2>
          <p class="finish__sub">{{ detail?.title || '' }}</p>

          <div class="finish__cost glass glass--thin">
            <span class="finish__cost-label">实际耗时（自动计时）</span>
            <span class="finish__cost-num">{{ costMinutes }}<i>分钟</i></span>
          </div>

          <div class="finish__field">
            <label class="finish__label" for="finish-stuck">卡在第几步</label>
            <select id="finish-stuck" v-model="stuckStep" class="select">
              <option :value="null">无</option>
              <option v-for="step in steps" :key="step.stepNo" :value="step.stepNo">
                第 {{ step.stepNo }} 步 · {{ step.title }}
              </option>
            </select>
          </div>

          <div class="finish__field">
            <label class="finish__label" for="finish-dev">发现什么问题</label>
            <textarea
              id="finish-dev"
              v-model="deviation"
              class="input input--area"
              rows="3"
              maxlength="500"
              placeholder="哪里卡住了、下次怎么改进（可选）"
            />
          </div>

          <div class="finish__actions">
            <button type="button" class="btn-ghost glass glass--thin" @click="cancelFinish">
              继续执行
            </button>
            <button
              type="button"
              class="btn-primary"
              :disabled="submitting"
              :aria-busy="submitting"
              @click="confirmFinish"
            >
              <span v-if="submitting" class="spinner" aria-hidden="true" />
              <span>{{ submitting ? '记录中…' : '完成并回列表' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.sop-runner {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* ---- 顶栏 ---- */
.runner-bar {
  position: sticky;
  top: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--r-lg);
}

.back-btn {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  padding: 0;
  color: var(--text-primary);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.runner-bar__center {
  flex: 1 1 auto;
  min-width: 0;
}

.runner-bar__title {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}

.progress {
  height: 5px;
  margin-top: 8px;
  overflow: hidden;
  background: var(--separator);
  border-radius: var(--r-pill);
}

.progress__fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #3d9bff, var(--accent));
  border-radius: var(--r-pill);
  transition: width var(--dur) var(--spring);
}

.timer {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  color: var(--text-primary);
  border-radius: var(--r-pill);
}

.timer__text {
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* ---- 步骤清单 ---- */
.runner-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  list-style: none;
}

.run-step {
  display: flex;
  gap: 14px;
  padding: 18px 20px;
  border-radius: var(--r-lg);
  transition: opacity var(--dur) var(--spring), transform var(--dur) var(--spring),
    box-shadow var(--dur) var(--spring), border-color var(--dur) var(--spring);
}

/* 未到步：普通态 */
.run-step--todo {
  opacity: 0.72;
}

/* 当前步：放大提亮 + 左侧主色竖条 */
.run-step--current {
  transform: scale(1.015);
  border-color: var(--accent);
  box-shadow: var(--glass-shadow), var(--glass-inner), 0 12px 34px rgba(0, 122, 255, 0.2);
}

.run-step--current::after {
  box-shadow: var(--glass-edge), inset 3px 0 0 0 var(--accent);
}

/* 已完成步：淡化 */
.run-step--done {
  opacity: 0.5;
}

.run-step--clickable {
  cursor: pointer;
}

.run-step__status {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
}

.run-step--current .run-step__status {
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 60%, var(--accent-press) 100%);
  border-color: transparent;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.42);
}

.run-step--done .run-step__status {
  color: var(--success);
  border-color: var(--success);
}

.run-step__body {
  flex: 1 1 auto;
  min-width: 0;
}

.run-step__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.run-step__title {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-primary);
}

.run-step__est {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--text-tertiary);
}

.run-step__detail {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  color: var(--text-secondary);
}

.run-step__done-hint {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
  color: var(--success);
}

/* ---- 避坑黄条（浅黄底 + 深黄左边框 + 感叹图标；明暗两套 token） ---- */
.run-tip {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-top: 12px;
  padding: 10px 13px;
  background: var(--warn-bg);
  border: 1px solid var(--warn-border);
  border-left: 3px solid var(--warn-border-strong);
  border-radius: var(--r-sm);
}

.run-tip__icon {
  flex: 0 0 auto;
  margin-top: 1px;
  color: var(--warn-icon);
}

.run-tip__label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: var(--warn-text);
}

.run-tip__text {
  margin-top: 3px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--warn-text);
}

/* ---- 底部导航 ---- */
.runner-foot {
  position: sticky;
  bottom: 16px;
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.nav-btn {
  display: inline-flex;
  flex: 1 1 0;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.nav-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.nav-btn--primary {
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 55%, var(--accent-press) 100%);
  border: 0;
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.42);
}

/* ---- 结束弹窗 ---- */
.finish {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.3);
  -webkit-backdrop-filter: blur(6px);
  backdrop-filter: blur(6px);
}

.finish__card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: min(460px, 100%);
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  padding: 28px 26px 24px;
  text-align: center;
  border-radius: var(--r-xl);
}

.finish__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  margin: 0 auto 14px;
  color: var(--success);
  border-radius: var(--r-pill);
}

.finish__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}

.finish__sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.finish__cost {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0;
  padding: 14px 18px;
  border-radius: var(--r-md);
}

.finish__cost-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.finish__cost-num {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.5px;
  color: var(--accent);
}

.finish__cost-num i {
  margin-left: 4px;
  font-size: 13px;
  font-style: normal;
  font-weight: 500;
  color: var(--text-tertiary);
}

.finish__field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 14px;
  text-align: left;
}

.finish__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.select {
  width: 100%;
  height: 46px;
  padding: 0 14px;
  font-size: 15px;
  font-family: var(--font-sans);
  color: var(--text-primary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-md);
  outline: none;
}

.select:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.18);
}

/* 深色下原生下拉选项文字要可读 */
html[data-theme='dark'] .select option {
  color: #000;
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

.input--area {
  height: auto;
  padding: 11px 15px;
  line-height: 1.5;
  resize: vertical;
}

.finish__actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.btn-ghost {
  flex: 1 1 0;
  height: 48px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.btn-primary {
  display: inline-flex;
  flex: 1.3 1 0;
  align-items: center;
  justify-content: center;
  gap: 9px;
  height: 48px;
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

/* ---- 弹窗过渡 ---- */
@media (prefers-reduced-motion: no-preference) {
  .po-finish-enter-active,
  .po-finish-leave-active {
    transition: opacity var(--dur) var(--spring);
  }

  .po-finish-enter-active .finish__card,
  .po-finish-leave-active .finish__card {
    transition: transform var(--dur-slow) var(--spring);
  }

  .po-finish-enter-from,
  .po-finish-leave-to {
    opacity: 0;
  }

  .po-finish-enter-from .finish__card,
  .po-finish-leave-to .finish__card {
    transform: translateY(16px) scale(0.98);
  }
}

/* ---- 空内联 ---- */
.empty-inline {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 24px;
  text-align: center;
  border-radius: var(--r-lg);
}

.empty-inline__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.muted {
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 480px) {
  .runner-bar {
    padding: 10px 12px;
  }

  .run-step {
    padding: 16px 16px;
  }

  .run-step--current {
    transform: none;
  }

  .run-step__title {
    font-size: 16px;
  }

  .runner-foot {
    bottom: 12px;
  }

  .nav-btn {
    height: 48px;
    font-size: 15px;
  }

  .finish__card {
    padding: 24px 20px 20px;
    border-radius: var(--r-lg);
  }

  .finish__cost-num {
    font-size: 26px;
  }

  .finish__actions {
    flex-direction: column-reverse;
  }
}
</style>
