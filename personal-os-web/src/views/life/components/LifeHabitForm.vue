<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { HabitSaveParams, HabitType, HabitVO } from '@/api/life'

/**
 * 生活域 · 习惯编辑表单（新建 / 编辑共用）。
 *
 * 极简：名称 + 类型（打卡型 / 计数型）+ 单位（仅计数型）+ 目标（仅计数型）。
 * 校验失败加 `aria-invalid`，错误提示 `role="alert"` + `aria-live`。
 */
const props = withDefaults(
  defineProps<{
    /** 编辑目标；null = 新建 */
    habit: HabitVO | null
    /** 提交中 */
    saving?: boolean
  }>(),
  {
    saving: false
  }
)

const emit = defineEmits<{
  /** 提交保存 */
  (e: 'submit', payload: HabitSaveParams): void
  /** 取消（用于编辑态收起） */
  (e: 'cancel'): void
}>()

/** 是否编辑态 */
const isEdit = computed<boolean>(() => props.habit !== null)

/** 表单模型（编辑态用传入值初始化） */
const form = reactive<{
  name: string
  type: HabitType
  unit: string
  targetValue: string
}>({
  name: props.habit?.name ?? '',
  type: props.habit?.type ?? 'checkin',
  unit: props.habit?.unit ?? '',
  targetValue: props.habit?.targetValue != null ? String(props.habit.targetValue) : ''
})

/** 校验标记 */
const nameInvalid = ref<boolean>(false)
const targetInvalid = ref<boolean>(false)

/** 错误文案 */
const errorText = ref<string>('')

/** 类型选项 */
const typeOptions: { value: HabitType; label: string }[] = [
  { value: 'checkin', label: '打卡型' },
  { value: 'count', label: '计数型' }
]

/** 清错 */
function clearError(): void {
  nameInvalid.value = false
  targetInvalid.value = false
  errorText.value = ''
}

/** 切换类型时清错 */
function handleTypeChange(type: HabitType): void {
  form.type = type
  clearError()
}

/** 校验并提交 */
function handleSubmit(): void {
  clearError()

  const name = form.name.trim()
  if (!name) {
    nameInvalid.value = true
    errorText.value = '请输入习惯名称'
    return
  }

  const payload: HabitSaveParams = { name, type: form.type }

  if (form.type === 'count') {
    const unit = form.unit.trim()
    if (unit) {
      payload.unit = unit
    }
    const target = Number(form.targetValue)
    if (!Number.isFinite(target) || target <= 0) {
      targetInvalid.value = true
      errorText.value = '计数型需填写大于 0 的目标值'
      return
    }
    payload.targetValue = target
  }

  emit('submit', payload)
}
</script>

<template>
  <form class="habit-form glass glass--thin" novalidate @submit.prevent="handleSubmit">
    <div class="habit-form__row">
      <div class="habit-field">
        <label class="habit-field__label" for="life-habit-name">名称</label>
        <input
          id="life-habit-name"
          v-model="form.name"
          class="habit-field__input"
          type="text"
          name="habitName"
          placeholder="如 喝水 / 跑步"
          autocomplete="off"
          :aria-invalid="nameInvalid"
          @input="clearError"
        />
      </div>

      <div class="habit-field">
        <span class="habit-field__label" id="life-habit-type-label">类型</span>
        <div class="habit-seg" role="radiogroup" aria-labelledby="life-habit-type-label">
          <button
            v-for="opt in typeOptions"
            :key="opt.value"
            type="button"
            class="habit-seg__item"
            role="radio"
            :aria-checked="form.type === opt.value"
            :class="{ 'habit-seg__item--on': form.type === opt.value }"
            @click="handleTypeChange(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="form.type === 'count'" class="habit-form__row">
      <div class="habit-field">
        <label class="habit-field__label" for="life-habit-unit">单位</label>
        <input
          id="life-habit-unit"
          v-model="form.unit"
          class="habit-field__input"
          type="text"
          name="habitUnit"
          placeholder="如 杯 / 公里"
          autocomplete="off"
        />
      </div>

      <div class="habit-field">
        <label class="habit-field__label" for="life-habit-target">每日目标</label>
        <input
          id="life-habit-target"
          v-model="form.targetValue"
          class="habit-field__input"
          type="number"
          min="1"
          step="1"
          inputmode="numeric"
          name="habitTarget"
          placeholder="如 8"
          :aria-invalid="targetInvalid"
          @input="clearError"
        />
      </div>
    </div>

    <p v-if="errorText" class="habit-form__error" role="alert" aria-live="assertive">
      {{ errorText }}
    </p>

    <div class="habit-form__actions">
      <button
        v-if="isEdit"
        type="button"
        class="habit-form__btn habit-form__btn--ghost"
        :disabled="props.saving"
        @click="emit('cancel')"
      >
        取消
      </button>
      <button
        type="submit"
        class="habit-form__btn habit-form__btn--primary"
        :disabled="props.saving"
        :aria-busy="props.saving"
      >
        {{ props.saving ? '保存中…' : isEdit ? '保存修改' : '新建习惯' }}
      </button>
    </div>
  </form>
</template>

<style scoped lang="scss">
.habit-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: var(--r-md);
}

.habit-form__row {
  display: flex;
  gap: 12px;
}

.habit-field {
  display: flex;
  flex: 1 1 0;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
}

.habit-field__label {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.2px;
  color: var(--text-tertiary);
}

.habit-field__input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-sm);
  transition: border-color var(--dur) var(--spring), box-shadow var(--dur) var(--spring);
}

.habit-field__input::placeholder {
  color: var(--text-tertiary);
}

.habit-field__input:focus-visible {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.18);
}

/* 分段控件 */
.habit-seg {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-sm);
}

.habit-seg__item {
  flex: 1 1 0;
  height: 30px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-sm);
  cursor: pointer;
  transition: color var(--dur) var(--spring), background-color var(--dur) var(--spring);
}

.habit-seg__item--on {
  color: var(--accent);
  background: var(--glass-bg-thick);
}

.habit-form__error {
  font-size: 12px;
  color: var(--danger);
}

.habit-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.habit-form__btn {
  height: 38px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: transform var(--dur-fast) var(--spring), opacity var(--dur) var(--spring);
}

.habit-form__btn--ghost {
  color: var(--text-primary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
}

.habit-form__btn--primary {
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 55%, var(--accent-press) 100%);
  border: 0;
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.42);
}

.habit-form__btn:disabled {
  cursor: not-allowed;
  opacity: 0.66;
}

@media (prefers-reduced-motion: no-preference) {
  .habit-form__btn:not(:disabled):active {
    transform: scale(0.97);
  }
}

@media (max-width: 480px) {
  .habit-form__row {
    flex-direction: column;
  }
}
</style>
