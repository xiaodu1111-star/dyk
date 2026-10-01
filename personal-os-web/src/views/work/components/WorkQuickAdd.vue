<script setup lang="ts">
import { ref } from 'vue'
import dayjs from 'dayjs'
import type { TaskCreateParams } from '@/views/work/types'
import WorkTagPicker from './WorkTagPicker.vue'

/**
 * 快速添加：输入框常驻顶部，回车即建（仅标题必填，对齐 15 秒硬指标）。
 * 可展开高级项：优先级 / 截止时间 / 预估工时 / 标签。
 */

const emit = defineEmits<{
  (e: 'submit', payload: TaskCreateParams): void
}>()

/** 标题 */
const title = ref('')

/** 是否展开高级项 */
const expanded = ref(false)

/** 提交中 */
const submitting = ref(false)

/** 高级项 */
const priority = ref(2)
const dueAt = ref('')
const estimateMin = ref<number | null>(null)
const tagIds = ref<number[]>([])

async function handleSubmit(): Promise<void> {
  const value = title.value.trim()
  if (!value || submitting.value) {
    return
  }
  submitting.value = true
  const payload: TaskCreateParams = { title: value, priority: priority.value }
  if (dueAt.value) {
    // <input type="datetime-local"> 产出 ISO 'T' 格式，后端 LocalDateTime 反序列化要求 'yyyy-MM-dd HH:mm:ss'
    payload.dueAt = dayjs(dueAt.value).format('YYYY-MM-DD HH:mm:ss')
  }
  if (estimateMin.value !== null && estimateMin.value >= 0) {
    payload.estimateMin = estimateMin.value
  }
  if (tagIds.value.length) {
    payload.tagIds = [...tagIds.value]
  }
  try {
    emit('submit', payload)
    reset()
  } finally {
    submitting.value = false
  }
}

/** 重置表单（保留展开态以便连续添加） */
function reset(): void {
  title.value = ''
  dueAt.value = ''
  estimateMin.value = null
  tagIds.value = []
  priority.value = 2
}

function toggleExpand(): void {
  expanded.value = !expanded.value
}
</script>

<template>
  <div class="quick-add glass glass--thin">
    <div class="quick-add__bar">
      <input
        v-model="title"
        class="quick-add__input"
        type="text"
        placeholder="添加任务，回车即建"
        aria-label="任务标题"
        :disabled="submitting"
        @keyup.enter.prevent="handleSubmit"
      />
      <button
        type="button"
        class="quick-add__expand"
        :aria-expanded="expanded"
        aria-label="展开高级选项"
        @click="toggleExpand"
      >
        <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none"
          stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path :d="expanded ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'" />
        </svg>
      </button>
    </div>

    <div v-if="expanded" class="quick-add__advanced">
      <div class="field">
        <label class="field__label" for="qa-priority">优先级</label>
        <div class="seg glass glass--thin" role="radiogroup" aria-label="优先级">
          <button
            v-for="p in [1, 2, 3]"
            :key="p"
            type="button"
            class="seg__item"
            role="radio"
            :aria-checked="priority === p"
            @click="priority = p"
          >
            {{ p === 1 ? '高' : p === 2 ? '中' : '低' }}
          </button>
        </div>
      </div>

      <div class="field">
        <label class="field__label" for="qa-due">截止时间</label>
        <input id="qa-due" v-model="dueAt" class="field__input glass glass--thin" type="datetime-local" />
      </div>

      <div class="field">
        <label class="field__label" for="qa-estimate">预估工时（分钟）</label>
        <input
          id="qa-estimate"
          v-model.number="estimateMin"
          class="field__input glass glass--thin"
          type="number"
          min="0"
          placeholder="如 30"
        />
      </div>

      <div class="field field--full">
        <span class="field__label">标签</span>
        <WorkTagPicker v-model="tagIds" />
      </div>

      <button type="button" class="quick-add__submit" :disabled="submitting" @click="handleSubmit">
        添加任务
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.quick-add {
  padding: 12px 14px;
  border-radius: var(--r-lg);
}

.quick-add__bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-add__input {
  flex: 1 1 auto;
  min-width: 0;
  height: 40px;
  padding: 0 4px;
  font-size: 15px;
  color: var(--text-primary);
  background: transparent;
  border: 0;
}

.quick-add__input::placeholder {
  color: var(--text-tertiary);
}

.quick-add__input:focus-visible {
  outline: none;
}

.quick-add__expand {
  display: inline-flex;
  flex: 0 0 auto;
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
  @include pressable;
}

.quick-add__advanced {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--separator);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field--full {
  grid-column: 1 / -1;
}

.field__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.field__input {
  height: 40px;
  padding: 0 12px;
  font-size: 14px;
  color: var(--text-primary);
  border-radius: var(--r-sm);
}

.field__input:focus-visible {
  outline: none;
  border-color: var(--accent);
}

.seg {
  display: flex;
  padding: 3px;
  border-radius: var(--r-pill);
}

.seg__item {
  flex: 1 1 0;
  height: 32px;
  font-size: 13px;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring), background-color var(--dur) var(--spring);
}

.seg__item[aria-checked='true'] {
  color: #fff;
  background: var(--accent);
}

.quick-add__submit {
  grid-column: 1 / -1;
  height: 42px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: var(--accent);
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.quick-add__submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

@media (max-width: 480px) {
  .quick-add__advanced {
    grid-template-columns: 1fr;
  }
}
</style>
