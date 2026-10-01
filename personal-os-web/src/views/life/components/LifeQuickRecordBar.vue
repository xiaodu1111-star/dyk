<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * 生活域 · 快捷记录输入条（顶部常驻）。
 *
 * - 输入如「跑步 5」回车 → 触发 `submit`，由父组件调 `/life/quick-record` 解析；
 * - 解析失败（14003）由父组件把文案回填到 `errorMessage`，本组件在输入框下方内联展示；
 * - 提交中禁用，显示 spinner。
 */
const props = withDefaults(
  defineProps<{
    /** 解析中 */
    loading?: boolean
    /** 内联错误提示（如「没认出来，试试『跑步 5』」） */
    errorMessage?: string
  }>(),
  {
    loading: false,
    errorMessage: ''
  }
)

const emit = defineEmits<{
  /** 提交解析文本 */
  (e: 'submit', text: string): void
  /** 输入变化（父组件据此清空内联错误） */
  (e: 'clear-error'): void
}>()

const text = ref<string>('')

/** 是否可提交 */
const canSubmit = computed<boolean>(() => text.value.trim().length > 0 && !props.loading)

/** 回车 / 点击提交 */
function handleSubmit(): void {
  if (!canSubmit.value) {
    return
  }
  emit('submit', text.value.trim())
}

/** 输入时清空错误 */
function handleInput(): void {
  if (props.errorMessage) {
    emit('clear-error')
  }
}
</script>

<template>
  <div class="quick-bar">
    <form class="quick-bar__row" novalidate @submit.prevent="handleSubmit">
      <label class="sr-only" for="life-quick-input">快捷记录</label>
      <div class="quick-bar__control glass glass--thin">
        <svg
          class="quick-bar__icon"
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="1.7"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M4 7h16M4 12h10M4 17h7" />
        </svg>
        <input
          id="life-quick-input"
          v-model="text"
          class="quick-bar__input"
          type="text"
          name="quickRecord"
          placeholder="记一笔，如「跑步 5」"
          autocomplete="off"
          autocapitalize="off"
          spellcheck="false"
          :aria-invalid="Boolean(props.errorMessage)"
          aria-describedby="life-quick-error"
          @input="handleInput"
        />
        <button
          type="submit"
          class="quick-bar__submit"
          :disabled="!canSubmit"
          :aria-label="props.loading ? '解析中' : '解析快捷记录'"
        >
          <span v-if="props.loading" class="spinner" aria-hidden="true" />
          <svg
            v-else
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M5 12h13M12.4 5.6 18.8 12l-6.4 6.4" />
          </svg>
        </button>
      </div>
    </form>

    <Transition name="po-life-hint">
      <p
        v-if="props.errorMessage"
        id="life-quick-error"
        class="quick-bar__error"
        role="alert"
        aria-live="assertive"
      >
        <svg
          class="quick-bar__error-icon"
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
        <span>没认出来，试试「跑步 5」</span>
      </p>
    </Transition>
  </div>
</template>

<style scoped lang="scss">
.quick-bar {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.quick-bar__control {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 48px;
  padding: 0 8px 0 16px;
  border-radius: var(--r-pill);
  transition: border-color var(--dur) var(--spring), box-shadow var(--dur) var(--spring);
}

.quick-bar__control:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.18), var(--glass-inner);
}

.quick-bar__icon {
  flex: 0 0 auto;
  color: var(--text-tertiary);
}

.quick-bar__input {
  flex: 1 1 auto;
  min-width: 0;
  height: 100%;
  padding: 0;
  font-size: 15px;
  color: var(--text-primary);
  background: transparent;
  border: 0;
}

.quick-bar__input::placeholder {
  color: var(--text-tertiary);
}

.quick-bar__input:focus-visible {
  outline: none;
}

.quick-bar__submit {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: var(--accent);
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: transform var(--dur-fast) var(--spring), opacity var(--dur) var(--spring);
}

.quick-bar__submit:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

@media (prefers-reduced-motion: no-preference) {
  .quick-bar__submit:not(:disabled):active {
    transform: scale(0.94);
  }
}

.quick-bar__error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 4px;
  font-size: 12px;
  color: var(--danger);
}

.quick-bar__error-icon {
  flex: 0 0 auto;
}

@media (prefers-reduced-motion: no-preference) {
  .po-life-hint-enter-active,
  .po-life-hint-leave-active {
    transition: opacity var(--dur) var(--spring), transform var(--dur) var(--spring);
  }

  .po-life-hint-enter-from,
  .po-life-hint-leave-to {
    opacity: 0;
    transform: translateY(-4px);
  }
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--separator);
  border-top-color: var(--accent);
  border-radius: 50%;
}

@media (prefers-reduced-motion: no-preference) {
  .spinner {
    animation: po-life-quick-spin 0.7s linear infinite;
  }
}

@keyframes po-life-quick-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
