<script setup lang="ts">
import { computed, ref } from 'vue'
import LifeFlameBadge from './LifeFlameBadge.vue'
import type { HabitVO } from '@/api/life'

/**
 * 生活域 · 习惯卡（打卡型 / 计数型二合一）。
 *
 * 视觉规格（docs/modules/m3-life-domain.md §5.1）：
 * - 打卡型：中央大圆形打卡钮（直径约 72px 玻璃描边圆），未打卡空心，
 *   打卡后实心填充 + 缩放波纹反馈；
 * - 计数型：中央大数字 `3/8`（数值大、目标小），下方细进度条，
 *   配「+1」玻璃按钮，满额后数字变主色 + 对勾角标；
 * - streak 角标固定在卡片右上角（自绘 SVG 火焰 + 数字）。
 *
 * 动效全部包在 `prefers-reduced-motion: no-preference`，reduce 下静止。
 */
const props = defineProps<{
  /** 习惯 VO */
  habit: HabitVO
  /** 连续天数 */
  streak: number
  /** 是否正在打卡（显示 spinner 并禁用交互） */
  checking: boolean
}>()

const emit = defineEmits<{
  /** 触发打卡（打卡型一次；计数型 increment=1） */
  (e: 'checkin', habit: HabitVO): void
  /** 计数型加量（增量可自定义） */
  (e: 'increment', habit: HabitVO, amount: number): void
}>()

/** 计数型目标（缺省 1，避免除零） */
const target = computed<number>(() => {
  const t = props.habit.targetValue ?? 1
  return t > 0 ? t : 1
})

/** 计数型进度（0–1 夹紧） */
const progress = computed<number>(() => {
  const ratio = props.habit.value / target.value
  return Math.max(0, Math.min(1, ratio))
})

/** 计数型是否满额 */
const full = computed<boolean>(() => props.habit.type === 'count' && props.habit.value >= target.value)

/** 打卡型是否已完成 */
const checked = computed<boolean>(() => props.habit.type === 'checkin' && props.habit.done)

/** 计数型展示的当前值（整数化） */
const currentValue = computed<number>(() => Math.round(props.habit.value))

/** 打卡型按钮的无障碍标签 */
const checkinLabel = computed<string>(() =>
  checked.value ? `${props.habit.name} 今日已完成` : `打卡 ${props.habit.name}`
)

/** 计数型是否展示单位 */
const unitText = computed<string>(() => props.habit.unit ?? '')

/** 波纹触发标记（打卡瞬间置 true，动画结束后复位） */
const ripple = ref<boolean>(false)

/** 打卡点击 */
function handleCheckin(): void {
  if (props.checking || checked.value) {
    return
  }
  triggerRipple()
  emit('checkin', props.habit)
}

/** 计数型 +1 */
function handleIncrement(): void {
  if (props.checking) {
    return
  }
  emit('increment', props.habit, 1)
}

/** 触发一次波纹（仅在允许动效时通过 CSS 呈现） */
function triggerRipple(): void {
  ripple.value = false
  // 下一帧重置，保证连续点击也能重新播放
  window.requestAnimationFrame(() => {
    ripple.value = true
    window.setTimeout(() => {
      ripple.value = false
    }, 520)
  })
}
</script>

<template>
  <article class="habit-card glass" :class="{ 'habit-card--done': checked || full }">
    <!-- streak 角标：卡片右上角 -->
    <LifeFlameBadge class="habit-card__flame" :streak="props.streak" :active="checked || full" />

    <h3 class="habit-card__name">{{ props.habit.name }}</h3>

    <!-- ---------- 打卡型 ---------- -->
    <template v-if="props.habit.type === 'checkin'">
      <button
        type="button"
        class="checkin-btn"
        :class="{ 'checkin-btn--done': checked }"
        :aria-pressed="checked"
        :aria-label="checkinLabel"
        :disabled="props.checking || checked"
        @click="handleCheckin"
      >
        <span v-if="ripple" class="checkin-btn__ripple" aria-hidden="true" />
        <span v-if="props.checking" class="spinner" aria-hidden="true" />
        <svg
          v-else-if="checked"
          class="checkin-btn__check"
          viewBox="0 0 24 24"
          width="30"
          height="30"
          fill="none"
          stroke="currentColor"
          stroke-width="2.6"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M5 12.6 9.6 17 19 7.2" />
        </svg>
      </button>
      <p class="habit-card__hint">
        {{ checked ? '今日已完成' : '点按完成打卡' }}
      </p>
    </template>

    <!-- ---------- 计数型 ---------- -->
    <template v-else>
      <div class="count-figure" :class="{ 'count-figure--full': full }">
        <span class="count-figure__value">{{ currentValue }}</span>
        <span class="count-figure__slash" aria-hidden="true">/</span>
        <span class="count-figure__target">{{ target }}</span>
        <svg
          v-if="full"
          class="count-figure__tick"
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="2.8"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M5 12.6 9.6 17 19 7.2" />
        </svg>
      </div>
      <span v-if="unitText" class="count-figure__unit">{{ unitText }}</span>

      <div
        class="count-bar"
        role="progressbar"
        :aria-valuenow="currentValue"
        aria-valuemin="0"
        :aria-valuemax="target"
        :aria-label="`${props.habit.name} 完成进度`"
      >
        <span class="count-bar__fill" :style="{ width: `${progress * 100}%` }" />
      </div>

      <button
        type="button"
        class="inc-btn glass glass--thin"
        :disabled="props.checking"
        :aria-label="`${props.habit.name} 加 1`"
        @click="handleIncrement"
      >
        <span v-if="props.checking" class="spinner spinner--accent" aria-hidden="true" />
        <span v-else>+1</span>
      </button>
    </template>
  </article>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.habit-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 22px 18px 18px;
  border-radius: var(--r-lg);
  @include enter-rise(12px);
}

.habit-card--done {
  border-color: rgba(52, 199, 89, 0.35);
}

.habit-card__flame {
  position: absolute;
  top: 12px;
  right: 12px;
}

.habit-card__name {
  max-width: 100%;
  font-size: 15px;
  font-weight: 600;
  text-align: center;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}

.habit-card__hint {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-tertiary);
}

/* ---------- 打卡型大圆钮（约 72px） ---------- */
.checkin-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-top: 2px;
  padding: 0;
  color: var(--text-tertiary);
  background: var(--glass-bg-thin);
  border: 2px solid var(--glass-border);
  border-radius: 50%;
  cursor: pointer;
  overflow: visible;
  -webkit-backdrop-filter: blur(14px);
  backdrop-filter: blur(14px);
  box-shadow: var(--glass-inner);
  transition: transform var(--dur-fast) var(--spring), color var(--dur) var(--spring),
    border-color var(--dur) var(--spring), background-color var(--dur) var(--spring),
    box-shadow var(--dur) var(--spring);
}

@media (prefers-reduced-motion: no-preference) {
  .checkin-btn:not(:disabled):active {
    transform: scale(0.94);
  }
}

.checkin-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 3px;
}

/* 完成后实心填充 + 主色 */
.checkin-btn--done {
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 55%, var(--accent-press) 100%);
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.42);
  cursor: default;
}

.checkin-btn:disabled {
  cursor: not-allowed;
}

/* 打卡瞬间的缩放波纹 */
.checkin-btn__ripple {
  position: absolute;
  inset: -1px;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(0, 122, 255, 0.34) 0%,
    rgba(0, 122, 255, 0.16) 45%,
    rgba(0, 122, 255, 0) 70%
  );
  pointer-events: none;
}

@media (prefers-reduced-motion: no-preference) {
  .checkin-btn__ripple {
    animation: po-life-ripple 520ms var(--spring) both;
  }
}

@keyframes po-life-ripple {
  from {
    opacity: 0.9;
    transform: scale(0.6);
  }
  to {
    opacity: 0;
    transform: scale(1.85);
  }
}

/* ---------- 计数型大数字 ---------- */
.count-figure {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  margin-top: 2px;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  transition: color var(--dur) var(--spring);
}

.count-figure__value {
  font-size: 40px;
  font-weight: 600;
  line-height: 1;
}

.count-figure__slash {
  margin: 0 1px;
  font-size: 22px;
  font-weight: 400;
  color: var(--text-tertiary);
}

.count-figure__target {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-secondary);
}

/* 满额：数字变主色 + 对勾角标 */
.count-figure--full {
  color: var(--accent);
}

.count-figure--full .count-figure__slash,
.count-figure--full .count-figure__target {
  color: var(--accent);
}

.count-figure__tick {
  align-self: center;
  margin-left: 4px;
  color: var(--accent);
}

.count-figure__unit {
  margin-top: -4px;
  font-size: 12px;
  color: var(--text-tertiary);
}

/* ---------- 细进度条 ---------- */
.count-bar {
  position: relative;
  width: 100%;
  height: 5px;
  margin-top: 2px;
  overflow: hidden;
  background: var(--separator);
  border-radius: var(--r-pill);
}

.count-bar__fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #4aa3ff 0%, var(--accent) 100%);
  border-radius: var(--r-pill);
  transition: width var(--dur-slow) var(--spring);
}

/* ---------- +1 按钮 ---------- */
.inc-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  height: 34px;
  margin-top: 2px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 600;
  color: var(--accent);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.inc-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.inc-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* ---------- spinner ---------- */
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: #fff;
  border-radius: 50%;
}

.spinner--accent {
  width: 16px;
  height: 16px;
  border-color: var(--separator);
  border-top-color: var(--accent);
}

@media (prefers-reduced-motion: no-preference) {
  .spinner {
    animation: po-life-spin 0.7s linear infinite;
  }
}

@keyframes po-life-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
