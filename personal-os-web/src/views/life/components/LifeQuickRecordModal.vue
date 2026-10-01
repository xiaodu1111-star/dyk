<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { QuickRecordVO } from '@/api/life'

/**
 * 生活域 · 快捷记录预填确认卡（浮层，玻璃模糊背景）。
 *
 * 数据流：父组件把 `/life/quick-record` 解析结果（QuickRecordVO）传入 →
 * 本组件把「指标名 / 数值 / 日期」三字段渲染为可编辑输入 →
 * 用户确认时 emit('confirm', { metricId, value, date }) 交父组件调 `/life/records`。
 *
 * 无障碍：`role="dialog"` + `aria-modal`，Esc 关闭，遮罩点击关闭，确认中禁用。
 */
const props = withDefaults(
  defineProps<{
    /** 是否展示 */
    modelValue: boolean
    /** 解析结果（null 时不展示内容） */
    parsed: QuickRecordVO | null
    /** 落库中 */
    saving?: boolean
  }>(),
  {
    saving: false
  }
)

const emit = defineEmits<{
  /** 开合同步 */
  (e: 'update:modelValue', value: boolean): void
  /** 确认落库 */
  (e: 'confirm', payload: { metricId: number; value: number; date: string }): void
}>()

/** 可编辑：指标名只读展示；数值 / 日期可改 */
const editValue = ref<string>('')
const editDate = ref<string>('')

/** 校验：数值必须为合法数字、日期必须为 yyyy-MM-dd */
const valueInvalid = ref<boolean>(false)
const dateInvalid = ref<boolean>(false)

/** 指标名（只读） */
const metricName = computed<string>(() => props.parsed?.metricName ?? '')

/** 同步外部解析结果到编辑态 */
watch(
  () => props.parsed,
  (next) => {
    if (next) {
      editValue.value = String(next.value ?? 1)
      editDate.value = next.date ?? ''
      valueInvalid.value = false
      dateInvalid.value = false
    }
  },
  { immediate: true }
)

/** 关闭 */
function close(): void {
  if (props.saving) {
    return
  }
  emit('update:modelValue', false)
}

/** 校验数值 */
function validateValue(): number | null {
  const n = Number(editValue.value)
  if (!Number.isFinite(n) || n <= 0) {
    valueInvalid.value = true
    return null
  }
  valueInvalid.value = false
  return n
}

/** 校验日期 */
function validateDate(): string | null {
  const ok = /^\d{4}-\d{2}-\d{2}$/.test(editDate.value.trim())
  if (!ok) {
    dateInvalid.value = true
    return null
  }
  dateInvalid.value = false
  return editDate.value.trim()
}

/** 确认 */
function handleConfirm(): void {
  if (!props.parsed || props.saving) {
    return
  }
  const value = validateValue()
  const date = validateDate()
  if (value === null || date === null) {
    return
  }
  emit('confirm', { metricId: props.parsed.metricId, value, date })
}

/** 输入时清错 */
function clearValueError(): void {
  valueInvalid.value = false
}

function clearDateError(): void {
  dateInvalid.value = false
}

/** Esc 关闭 */
function handleKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    close()
  }
}
</script>

<template>
  <Transition name="po-life-modal">
    <div
      v-if="props.modelValue && props.parsed"
      class="quick-modal"
      @keydown="handleKeydown"
    >
      <div class="quick-modal__scrim" aria-hidden="true" @click="close" />

      <section
        class="quick-modal__card glass glass--thick"
        role="dialog"
        aria-modal="true"
        aria-labelledby="life-quick-modal-title"
        tabindex="-1"
      >
        <header class="quick-modal__head">
          <h2 id="life-quick-modal-title" class="quick-modal__title">确认记录</h2>
          <button
            type="button"
            class="quick-modal__close"
            aria-label="关闭"
            :disabled="props.saving"
            @click="close"
          >
            <svg
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="none"
              stroke="currentColor"
              stroke-width="1.9"
              stroke-linecap="round"
              aria-hidden="true"
            >
              <path d="M6 6l12 12M18 6 6 18" />
            </svg>
          </button>
        </header>

        <dl class="quick-modal__fields">
          <div class="quick-field">
            <dt class="quick-field__label">指标</dt>
            <dd class="quick-field__static">{{ metricName }}</dd>
          </div>

          <div class="quick-field">
            <dt>
              <label class="quick-field__label" for="life-quick-value">数值</label>
            </dt>
            <dd>
              <input
                id="life-quick-value"
                v-model="editValue"
                class="quick-field__input glass glass--thin"
                type="number"
                min="0"
                step="1"
                inputmode="decimal"
                name="value"
                :aria-invalid="valueInvalid"
                :disabled="props.saving"
                @input="clearValueError"
              />
            </dd>
          </div>

          <div class="quick-field">
            <dt>
              <label class="quick-field__label" for="life-quick-date">日期</label>
            </dt>
            <dd>
              <input
                id="life-quick-date"
                v-model="editDate"
                class="quick-field__input glass glass--thin"
                type="date"
                name="date"
                :aria-invalid="dateInvalid"
                :disabled="props.saving"
                @input="clearDateError"
              />
            </dd>
          </div>
        </dl>

        <p v-if="valueInvalid || dateInvalid" class="quick-modal__error" role="alert" aria-live="assertive">
          <svg
            viewBox="0 0 24 24"
            width="15"
            height="15"
            fill="none"
            stroke="currentColor"
            stroke-width="1.9"
            stroke-linecap="round"
            aria-hidden="true"
          >
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7.6v5.2" />
            <path d="M12 16.4h.01" />
          </svg>
          <span>{{ valueInvalid ? '请输入大于 0 的数值' : '日期格式应为 YYYY-MM-DD' }}</span>
        </p>

        <footer class="quick-modal__actions">
          <button
            type="button"
            class="quick-modal__btn quick-modal__btn--ghost glass glass--thin"
            :disabled="props.saving"
            @click="close"
          >
            取消
          </button>
          <button
            type="button"
            class="quick-modal__btn quick-modal__btn--primary"
            :disabled="props.saving"
            :aria-busy="props.saving"
            @click="handleConfirm"
          >
            <span v-if="props.saving" class="spinner" aria-hidden="true" />
            <span>{{ props.saving ? '保存中…' : '确认记录' }}</span>
          </button>
        </footer>
      </section>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
.quick-modal {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.quick-modal__scrim {
  position: absolute;
  inset: 0;
  background: rgba(10, 14, 25, 0.32);
  -webkit-backdrop-filter: blur(8px) saturate(140%);
  backdrop-filter: blur(8px) saturate(140%);
}

.quick-modal__card {
  position: relative;
  z-index: 1;
  width: min(400px, calc(100vw - 40px));
  padding: 22px 22px 18px;
  border-radius: var(--r-xl);
}

.quick-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.quick-modal__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.quick-modal__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring), transform var(--dur-fast) var(--spring);
}

.quick-modal__close:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

@media (prefers-reduced-motion: no-preference) {
  .quick-modal__close:not(:disabled):active {
    transform: scale(0.94);
  }
}

.quick-modal__fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-field__label {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.2px;
  color: var(--text-tertiary);
}

.quick-field__static {
  height: 44px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 500;
  color: var(--accent);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  border-radius: var(--r-sm);
}

.quick-field__input {
  width: 100%;
  height: 44px;
  padding: 0 16px;
  font-size: 15px;
  color: var(--text-primary);
  border-radius: var(--r-sm);
  transition: border-color var(--dur) var(--spring), box-shadow var(--dur) var(--spring);
}

.quick-field__input:focus-visible {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.18);
}

/* 日期输入在深色下的日历图标可见性 */
.quick-field__input[type='date'] {
  color-scheme: light dark;
}

.quick-modal__error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--danger);
}

.quick-modal__actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.quick-modal__btn {
  flex: 1 1 auto;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: transform var(--dur-fast) var(--spring), opacity var(--dur) var(--spring);
}

.quick-modal__btn--ghost {
  color: var(--text-primary);
}

.quick-modal__btn--primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 55%, var(--accent-press) 100%);
  border: 0;
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.42);
}

.quick-modal__btn:disabled {
  cursor: not-allowed;
  opacity: 0.66;
}

@media (prefers-reduced-motion: no-preference) {
  .quick-modal__btn:not(:disabled):active {
    transform: scale(0.97);
  }
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-top-color: #fff;
  border-radius: 50%;
}

@media (prefers-reduced-motion: no-preference) {
  .spinner {
    animation: po-life-modal-spin 0.7s linear infinite;
  }
}

@keyframes po-life-modal-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 弹窗进出场 */
@media (prefers-reduced-motion: no-preference) {
  .po-life-modal-enter-active,
  .po-life-modal-leave-active {
    transition: opacity var(--dur) var(--spring);
  }

  .po-life-modal-enter-active .quick-modal__card,
  .po-life-modal-leave-active .quick-modal__card {
    transition: transform var(--dur) var(--spring), opacity var(--dur) var(--spring);
  }

  .po-life-modal-enter-from,
  .po-life-modal-leave-to {
    opacity: 0;
  }

  .po-life-modal-enter-from .quick-modal__card,
  .po-life-modal-leave-to .quick-modal__card {
    opacity: 0;
    transform: translateY(14px) scale(0.97);
  }
}

@media (max-width: 480px) {
  .quick-modal__card {
    width: calc(100vw - 32px);
    padding: 20px 18px 16px;
    border-radius: var(--r-lg);
  }
}
</style>
